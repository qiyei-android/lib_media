package com.qiyei.android.media.app.camera

import android.view.SurfaceHolder

/**
 * CameraLoader
 */
abstract class CameraLoader {

    protected var onPreviewFrame: ((data: ByteArray, width: Int, height: Int) -> Unit)? = null

    abstract fun onResume(width: Int, height: Int)

    abstract fun setPreview(holder: SurfaceHolder)

    abstract fun onPause()

    abstract fun switchCamera()

    abstract fun getCameraOrientation(): Int

    abstract fun hasMultipleCamera(): Boolean

    fun setOnPreviewFrameListener(onPreviewFrame: (data: ByteArray, width: Int, height: Int) -> Unit) {
        this.onPreviewFrame = onPreviewFrame
    }
}