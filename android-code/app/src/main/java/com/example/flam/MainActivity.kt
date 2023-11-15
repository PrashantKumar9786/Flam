package com.example.flam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Size
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.Button
import android.view.Gravity
import android.view.TextureView
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private lateinit var glViewContainer: FrameLayout
    private lateinit var glRendererView: FlamGLSurfaceView
    private lateinit var textureView: TextureView
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val REQ_CODE = 1001
    private var frameCount = 0
    private var lastFpsTsNs = System.nanoTime()
    private var showProcessed = true // Toggle between raw camera and processed view
    private lateinit var fpsTextView: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // create a container and add GLSurfaceView inside it
        glViewContainer = FrameLayout(this)
        glViewContainer.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setContentView(glViewContainer)

        // TextureView preview (base layer)
        textureView = TextureView(this)
        glViewContainer.addView(textureView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        glRendererView = FlamGLSurfaceView(this)
        glViewContainer.addView(glRendererView)
        
        // Add FPS text view (top-left)
        fpsTextView = android.widget.TextView(this).apply {
            text = "FPS: --"
            setTextColor(android.graphics.Color.WHITE)
            setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK) // Add shadow for visibility
            textSize = 16f
        }
        val fpsLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val marginPx = 32
            setMargins(marginPx, marginPx, marginPx, marginPx)
        }
        glViewContainer.addView(fpsTextView, fpsLp)

        // Toggle button (bottom-left)
        val toggleBtn = Button(this).apply {
            text = "Toggle View"
            alpha = 0.85f
            setOnClickListener {
                showProcessed = !showProcessed
                // Update visibility based on toggle state
                glRendererView.visibility = if (showProcessed) android.view.View.VISIBLE else android.view.View.INVISIBLE
                text = if (showProcessed) "Show Raw" else "Show Processed"
            }
        }
        val toggleLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            val marginPx = 32
            setMargins(marginPx, marginPx, marginPx, marginPx)
        }
        glViewContainer.addView(toggleBtn, toggleLp)

        // Visible on-screen Save button (bottom-right)
        val saveBtn = Button(this).apply {
            text = "Save Frame"
            alpha = 0.85f
        }
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            val marginPx = 32
            setMargins(marginPx, marginPx, marginPx, marginPx)
        }
        glViewContainer.addView(saveBtn, lp)

        saveBtn.setOnClickListener {
            // Save processed frame
            glRendererView.saveFrame(this) { processedPath ->
                // Also save raw camera frame
                saveRawCameraFrame { rawPath ->
                    runOnUiThread {
                        if (processedPath != null) {
                            val message = if (rawPath != null) {
                                "Saved processed: $processedPath\nRaw: $rawPath"
                            } else {
                                "Saved processed: $processedPath\nRaw: Failed to save"
                            }
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "No frame to save yet", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQ_CODE)
        } else {
            startCamera()
        }
    }

    private fun allPermissionsGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CODE) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()

            val targetRes = Size(640, 480) // change to 320x240 if performance is needed
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(targetRes)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // Provide TextureView surface to CameraX Preview
            preview.setSurfaceProvider { request ->
                val surfaceTexture = textureView.surfaceTexture
                if (surfaceTexture == null) {
                    // TextureView not ready yet; skip providing surface for this request
                    request.willNotProvideSurface()
                    return@setSurfaceProvider
                }
                val res = request.resolution
                surfaceTexture.setDefaultBufferSize(res.width, res.height)
                val surface = Surface(surfaceTexture)
                request.provideSurface(surface, cameraExecutor) {
                    surface.release()
                }
            }

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val nv21 = imageProxyToNV21(imageProxy)

                    // process in background thread (we are already on cameraExecutor)
                    try {
                        val outRgba: ByteArray = NativeUtils.processFrameNV21(nv21, imageProxy.width, imageProxy.height)
                        // push to GL view (safely via queue)
                        glRendererView.queueFrame(outRgba, imageProxy.width, imageProxy.height)
                        // FPS logging
                        logFps()
                    } catch (e: Throwable) {
                        // Native failed — push a blank frame to avoid crashes (graceful)
                        val fallback = ByteArray(imageProxy.width * imageProxy.height * 4) { 0.toByte() }
                        glRendererView.queueFrame(fallback, imageProxy.width, imageProxy.height)
                        logFps()
                    }
                } finally {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun imageProxyToNV21(image: ImageProxy): ByteArray {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y
        yBuffer.get(nv21, 0, ySize)

        // Copy VU (NV21 format is YVU)
        val uvPos = ySize
        if (image.planes[1].pixelStride == 2 && image.planes[2].pixelStride == 2) {
            // Interleaved UV planes
            val uPos = 0
            val vPos = 1
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride
            val uvWidth = image.width / 2
            val uvHeight = image.height / 2

            // Interleaved UV buffer position
            for (row in 0 until uvHeight) {
                for (col in 0 until uvWidth) {
                    val uvIndex = row * uvRowStride + col * uvPixelStride
                    nv21[uvPos + (row * uvWidth + col) * 2 + 1] = uBuffer[uvIndex + uPos]
                    nv21[uvPos + (row * uvWidth + col) * 2] = vBuffer[uvIndex + vPos]
                }
            }
        } else {
            // Non-interleaved planes, copy separately
            vBuffer.get(nv21, uvPos, vSize)
            uBuffer.get(nv21, uvPos + vSize, uSize)
        }

        return nv21
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        glRendererView.onDestroy()
    }

    private fun logFps() {
        frameCount++
        val now = System.nanoTime()
        val dtNs = now - lastFpsTsNs
        if (dtNs >= 1_000_000_000L) {
            val fps = frameCount * (1_000_000_000.0 / dtNs)
            val fpsText = String.format("FPS: %.1f", fps)
            Log.d("FlamFPS", fpsText)
            
            // Update UI on main thread
            runOnUiThread {
                fpsTextView.text = fpsText
            }
            
            frameCount = 0
            lastFpsTsNs = now
        }
    }
    
    private fun saveRawCameraFrame(callback: (String?) -> Unit) {
        // Check if TextureView is ready
        if (!textureView.isAvailable) {
            callback(null)
            return
        }
        
        try {
            // Get bitmap from TextureView
            val bitmap = textureView.bitmap ?: run {
                callback(null)
                return
            }
            
            // Save to app's external files directory with fixed filename
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "flam")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "raw_sample.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // Clean up bitmap
            bitmap.recycle()
            
            callback(file.absolutePath)
        } catch (e: Exception) {
            Log.e("FlamSave", "Error saving raw frame", e)
            callback(null)
        }
    }
}
