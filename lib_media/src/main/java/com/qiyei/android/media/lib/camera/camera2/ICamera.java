package com.qiyei.android.media.lib.camera.camera2;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

import java.util.List;

public interface ICamera {

    /**
     * 打开
     */
    void open();

    /**
     * 关闭
     */
    void close();

    /**
     * 准备
     */
    void prepare(CameraInfo cameraInfo);

    /**
     * 开始业务
     * @param cameraInfo
     * @param cameraDevice
     * @param captureSession
     */
    void start(CameraInfo cameraInfo,CameraDevice cameraDevice,CameraCaptureSession captureSession);

    /**
     * 停止业务
     */
    void stop();

    /**
     * 设置surface大小
     * @param cameraInfo
     * @param size
     */
    void setSurfaceSize(CameraInfo cameraInfo, Size size);

    /**
     * 获取所有的surface
     * @return
     */
    List<Surface> getSurfaces(CameraInfo cameraInfo);

    /**
     * 获取Handler
     * @return
     */
    Handler getHandler();

    /**
     * Image回调
     * @param image
     */
    void onImageCallBack(Image image);
}
