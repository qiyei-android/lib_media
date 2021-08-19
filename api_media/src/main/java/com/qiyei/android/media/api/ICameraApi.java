package com.qiyei.android.media.api;

import android.hardware.Camera;
import android.media.MediaCodec;
import android.view.SurfaceHolder;

import androidx.lifecycle.LiveData;

import java.lang.ref.WeakReference;

/**
 * 用于定义对外开放的操作摄像头的统一接口
 */
public interface ICameraApi {

    boolean open();

    boolean close();

    boolean start();

    boolean stop();

    /**
     * 设置打开的摄像头ID，默认值为 后置摄像头
     * @param id
     */
    void setCameraId(int id);

    /**
     * 监听摄像头的工作状态，注：组件生命周期感知
     * @return @CameraWorkingStatus
     */
    LiveData<Integer> getStatusLiveData();

    /**
     * 设置预览时的方向，横竖屏
     *
     * @param orientation     Configuration.ORIENTATION_PORTRAIT Configuration.ORIENTATION_LANDSCAPE
     * @param windowRotateDeg
     */
    void setPreviewOrientation(int orientation, int windowRotateDeg);

    /**
     * 设置预览的分辨率，支持内部自适应
     *
     * @param width
     * @param height
     * @return 实际的预览分辨率
     */
    int[] setPreviewResolution(int width, int height);

    void setSurfaceHolderRef(WeakReference<SurfaceHolder> surfaceHolderRef);

    void setPreviewCallback(Camera.PreviewCallback cb);

    MediaCodec getMediaCodec(String name, int encoderColorFormat);

    int[] getPreviewResolution();

    int getCameraOrientation();

    int getWindowDegree();

    void switchCamera();

    void release();
}
