package com.qiyei.android.media.lib.camera.camera2;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.MediaActionSound;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.qiyei.android.media.api.CameraUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class CameraRecordController extends AbsCameraController{

    /**
     * 快门声音
     */
    private MediaActionSound mMediaActionSound;


    /**
     * 设备方向监听
     */
    private DeviceOrientationListener mDeviceOrientationListener;


    /**
     * 捕获阻塞队列
     */
    private BlockingQueue<CaptureResult> mCaptureBlockingQueue = new LinkedBlockingDeque<>();


    public CameraRecordController(Context context, String tag) {
        super(context,tag,"CameraRecord");
        mDeviceOrientationListener = new DeviceOrientationListener(context);
        mDeviceOrientationListener.enable();
        mMediaActionSound = new MediaActionSound();

    }

    @Override
    public void onImageCallBack(Image image) {
        Log.i(mTag, subTag + "##### ImageAvailableListener onImageAvailable,planes=" + image.getPlanes());

    }

    @Override
    public void setSurfaceSize(CameraInfo cameraInfo, Size size) {
        super.setSurfaceSize(cameraInfo,size);
        mSurfaceSize = size;
        if (cameraInfo.map.isOutputSupportedFor(ImageFormat.YUV_420_888)){
            mImageReader = buildImageLoader(mTag,mSurfaceSize,ImageFormat.YUV_420_888,1);
        } else {
            Log.w(mTag,subTag + "mCameraInfo id=" + cameraInfo.cameraId + " not support format=" + ImageFormat.YUV_420_888);
        }
    }


    @Override
    public CaptureRequest buildCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice) {
        super.buildCaptureRequest(cameraInfo,cameraDevice);
        try {
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            for (Surface surface : getSurfaces()){
                mCaptureRequestBuilder.addTarget(surface);
            }

            int deviceOrientation = mDeviceOrientationListener.getOrientation();

            int jpegOrientation = CameraUtils.getImageOrientation(cameraInfo.characteristics,deviceOrientation);

            //设置角度
            //mCaptureRequestBuilder.set(CaptureRequest.OR,jpegOrientation);

            return mCaptureRequestBuilder.build();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void sendCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice,CameraCaptureSession captureSession) {
        super.sendCaptureRequest(cameraInfo,cameraDevice,captureSession);
        CaptureRequest request = buildCaptureRequest(cameraInfo,cameraDevice);
        if (request == null){
            Log.e(mTag,subTag + "sendCaptureRequest error, CaptureRequest is null");
            return;
        }
        try {
            captureSession.capture(request, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    //mMediaActionSound.play(MediaActionSound.SHUTTER_CLICK);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

//                    try {
//                        mCaptureBlockingQueue.put(result);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }

                }
            }, getHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
