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

    void setSurfaceSize(CameraInfo cameraInfo, Size size);

    List<Surface> getSurfaces();

    Handler getHandler();

    CaptureRequest buildCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice);

    void sendCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice,CameraCaptureSession captureSession);

    void onImageCallBack(Image image);

    void close();
}
