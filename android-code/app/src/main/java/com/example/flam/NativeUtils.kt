package com.example.flam

class NativeUtils {
    companion object {
        // Load native library
        init {
            System.loadLibrary("flam_native")
        }

        // Native method to process NV21 frame
        external fun processFrameNV21(nv21: ByteArray, width: Int, height: Int): ByteArray
    }
}
