#include <jni.h>
#include <string>
#include <android/log.h>

#ifdef HAVE_OPENCV
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
using namespace cv;
#endif

#define TAG "FlamNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Convert NV21 to BGR
void nv21ToBGR(const uint8_t* nv21, int width, int height, uint8_t* bgr) {
    const uint8_t* y = nv21;
    const uint8_t* uv = nv21 + width * height;

    for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {
            int y_idx = j * width + i;
            int uv_idx = (j >> 1) * width + (i & ~1);
            
            int Y = y[y_idx] & 0xff;
            int U = uv[uv_idx] & 0xff;
            int V = uv[uv_idx + 1] & 0xff;
            
            // YUV -> RGB conversion
            int y1192 = 1192 * (Y - 16);
            int r = (y1192 + 1634 * (V - 128)) >> 10;
            int g = (y1192 - 833 * (V - 128) - 400 * (U - 128)) >> 10;
            int b = (y1192 + 2066 * (U - 128)) >> 10;
            
            // Clamp
            r = r < 0 ? 0 : (r > 255 ? 255 : r);
            g = g < 0 ? 0 : (g > 255 ? 255 : g);
            b = b < 0 ? 0 : (b > 255 ? 255 : b);
            
            // BGR order (OpenCV format)
            bgr[y_idx * 3 + 0] = (uint8_t)b;
            bgr[y_idx * 3 + 1] = (uint8_t)g;
            bgr[y_idx * 3 + 2] = (uint8_t)r;
        }
    }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_flam_NativeUtils_processFrameNV21(
        JNIEnv* env,
        jclass /* clazz */,
        jbyteArray nv21_data,
        jint width,
        jint height) {

    // Get NV21 data from Java
    jbyte* nv21 = env->GetByteArrayElements(nv21_data, nullptr);
    if (nv21 == nullptr) {
        LOGE("Failed to get NV21 data from Java");
        return nullptr;
    }

    // Allocate memory for BGR image
    uint8_t* bgr = new uint8_t[width * height * 3];
    
    // Convert NV21 to BGR
    nv21ToBGR(reinterpret_cast<uint8_t*>(nv21), width, height, bgr);

    // Process the image
    uint8_t* processed = nullptr;
    
#ifdef HAVE_OPENCV
    // OpenCV processing
    try {
        // Create OpenCV Mat from BGR data
        Mat bgrMat(height, width, CV_8UC3, bgr);
        Mat grayMat, edgeMat, rgbaMat;
        
        // Convert to grayscale
        cvtColor(bgrMat, grayMat, COLOR_BGR2GRAY);
        
        // Apply Canny edge detection
        Canny(grayMat, edgeMat, 50, 150);
        
        // Convert to RGBA for output
        cvtColor(edgeMat, rgbaMat, COLOR_GRAY2RGBA);
        
        // Allocate output buffer
        processed = new uint8_t[width * height * 4];
        
        // Copy data to output buffer
        memcpy(processed, rgbaMat.data, width * height * 4);
    } catch (const std::exception& e) {
        LOGE("OpenCV error: %s", e.what());
        delete[] bgr;
        env->ReleaseByteArrayElements(nv21_data, nv21, JNI_ABORT);
        return nullptr;
    }
#else
    // Fallback processing (simple grayscale)
    processed = new uint8_t[width * height * 4];
    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            int idx = i * width + j;
            uint8_t gray = (bgr[idx * 3] + bgr[idx * 3 + 1] + bgr[idx * 3 + 2]) / 3;
            processed[idx * 4] = gray;     // R
            processed[idx * 4 + 1] = gray; // G
            processed[idx * 4 + 2] = gray; // B
            processed[idx * 4 + 3] = 255;  // A
        }
    }
#endif

    // Clean up BGR buffer
    delete[] bgr;
    
    // Release NV21 data
    env->ReleaseByteArrayElements(nv21_data, nv21, JNI_ABORT);
    
    // Create output byte array
    jbyteArray result = env->NewByteArray(width * height * 4);
    if (result == nullptr) {
        LOGE("Failed to create output byte array");
        delete[] processed;
        return nullptr;
    }
    
    // Copy processed data to output array
    env->SetByteArrayRegion(result, 0, width * height * 4, reinterpret_cast<jbyte*>(processed));
    
    // Clean up processed buffer
    delete[] processed;
    
    return result;
}
