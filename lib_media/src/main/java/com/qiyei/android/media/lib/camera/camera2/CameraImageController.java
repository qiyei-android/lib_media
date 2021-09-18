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

public class CameraImageController extends AbsCameraController{

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


    public CameraImageController(Context context,String tag) {
        super(context,tag,"CameraImage");
        mDeviceOrientationListener = new DeviceOrientationListener(context);
        mMediaActionSound = new MediaActionSound();
    }

    @Override
    public void prepare(CameraInfo cameraInfo) {
        //1 检查surface
        if (mImageReader == null){
            mImageReader = buildImageReader(mTag, mSurfaceSize,ImageFormat.JPEG,1);
        }
        mDeviceOrientationListener.enable();
    }

    @Override
    public void start(CameraInfo cameraInfo, CameraDevice cameraDevice, CameraCaptureSession captureSession) {
        CaptureRequest request = buildCaptureRequest(cameraInfo,cameraDevice);
        if (request == null){
            Log.e(mTag,subTag + "start error, CaptureRequest is null");
            return;
        }
        sendCaptureRequest(cameraInfo,cameraDevice,captureSession,request);
    }

    @Override
    public void stop() {
        super.stop();
        mDeviceOrientationListener.disable();
    }

    @Override
    public void onImageCallBack(Image image) {
        //Log.i(mTag, subTag + "##### ImageAvailableListener onImageAvailable,planes=" + image.getPlanes());
        try {
            CaptureResult captureResult = mCaptureBlockingQueue.take();
            ByteBuffer buffer = null;
            if (image != null && captureResult != null) {
                // Jpeg image data only occupy the planes[0].
                buffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                CameraUtils.writeToMediaStore(mTag, mContext, captureResult, data, image.getWidth(), image.getHeight());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setSurfaceSize(CameraInfo cameraInfo, Size size) {
        super.setSurfaceSize(cameraInfo,size);
        mSurfaceSize = size;
        if (cameraInfo.map.isOutputSupportedFor(ImageFormat.JPEG)){
            mImageReader = buildImageReader(mTag, mSurfaceSize,ImageFormat.JPEG,1);
        } else {
            Log.w(mTag,subTag + "mCameraInfo id=" + cameraInfo.cameraId + " not support format=" + ImageFormat.JPEG);
        }
    }


    @Override
    protected CaptureRequest buildCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice) {
        super.buildCaptureRequest(cameraInfo,cameraDevice);
        try {
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            setCameraAutoMode(cameraInfo,mCaptureRequestBuilder);
            for (Surface surface : getSurfaces(cameraInfo)){
                mCaptureRequestBuilder.addTarget(surface);
            }

            int deviceOrientation = mDeviceOrientationListener.getOrientation();

            int jpegOrientation = CameraUtils.getOrientation(cameraInfo.characteristics,deviceOrientation);

            //设置角度
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,jpegOrientation);

            // Configure the location information.
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_GPS_LOCATION,CameraUtils.getLocation(mContext));

            // Configure the image quality.
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_QUALITY,Byte.valueOf("100"));

            return mCaptureRequestBuilder.build();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void sendCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice,CameraCaptureSession captureSession,CaptureRequest request) {
        super.sendCaptureRequest(cameraInfo,cameraDevice,captureSession,request);
        try {
            captureSession.capture(request, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    mMediaActionSound.play(MediaActionSound.SHUTTER_CLICK);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    try {
                        mCaptureBlockingQueue.put(result);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }, getHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
