package com.example.flam

import android.content.Context
import android.opengl.GLSurfaceView

class FlamGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: GLRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = GLRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    /**
     * Called by MainActivity (camera thread) to push a new RGBA frame.
     * We copy the bytes into the renderer via queueEvent to run on GL thread.
     */
    fun queueFrame(rgbaBytes: ByteArray, width: Int, height: Int) {
        // copy the bytes to avoid writer/reader race
        val copy = rgbaBytes.copyOf()
        queueEvent {
            renderer.updateFrame(copy, width, height)
            requestRender()
        }
    }

    fun onDestroy() {
        // nothing special here for now; placeholder if cleanup needed later
    }

    fun saveFrame(context: Context, onSaved: (String?) -> Unit) {
        queueEvent {
            val path = renderer.saveLastFrame(context)
            onSaved(path)
        }
    }
}
