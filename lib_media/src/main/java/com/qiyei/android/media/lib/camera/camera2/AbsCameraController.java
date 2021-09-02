package com.qiyei.android.media.lib.camera.camera2;

import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;


public abstract class AbsCameraController implements ICamera{

    protected Context mContext;

    /**
     * 一级tag
     */
    protected String mTag;

    /**
     * 二级tag
     */
    protected String subTag;

    /**
     * 相机线程
     */
    protected HandlerThread mCameraThread;

    /**
     * 相机Handler
     */
    protected Handler mCameraHandler;

    /**
     * CaptureRequest请求
     */
    protected CaptureRequest.Builder mCaptureRequestBuilder;

    /**
     * surface尺寸
     */
    protected Size mSurfaceSize = new Size(1080,1920);

    /**
     * 预览Surface
     */
    protected Surface mPreviewSurface;

    /**
     * ImageReader
     */
    protected ImageReader mImageReader;

    /**
     * surface
     */
    protected List<Surface> mSurfaces;


    private ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            //Log.v(mTag, subTag + "##### ImageAvailableListener onImageAvailable,planes.length=" + image.getPlanes().length);
            onImageCallBack(image);
            image.close();
        }
    };


    public AbsCameraController(Context context,String tag,String subTag) {
        mContext = context;
        this.mTag = tag;
        this.subTag = subTag + " ";
    }

    @Override
    public void open() {
        mCameraThread = new HandlerThread(mTag);
        mCameraThread.start();

        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    @Override
    public void close() {
        mCaptureRequestBuilder = null;
        if (mSurfaces != null){
            mSurfaces.clear();
            mSurfaces = null;
        }
        if (mImageReader != null){
            mImageReader.close();
            mImageReader = null;
        }
        mCameraHandler = null;
        if (mCameraThread != null){
            mCameraThread.quitSafely();
            mCameraThread = null;
        }
    }

    @Override
    public Handler getHandler() {
        return mCameraHandler;
    }


    @Override
    public void setSurfaceSize(CameraInfo cameraInfo, Size size) {
        if (cameraInfo == null || cameraInfo.map == null || size == null){
            throw new IllegalArgumentException(subTag + "setSurfaceSize cameraInfo,map or size is null");
        }
    }

    @Override
    public List<Surface> getSurfaces(CameraInfo cameraInfo) {
        if (mSurfaces == null){
            mSurfaces = new ArrayList<>();
            if (mPreviewSurface != null){
                mSurfaces.add(mPreviewSurface);
            }
            if (mImageReader != null){
                mSurfaces.add(mImageReader.getSurface());
            }
        }
        return mSurfaces;
    }

    @Override
    public void stop() {
        if (mSurfaces != null){
            mSurfaces.clear();
            mSurfaces = null;
        }
    }

    protected CaptureRequest buildCaptureRequest(CameraInfo cameraInfo, CameraDevice cameraDevice) {
        if (cameraInfo == null || cameraDevice == null){
            throw new IllegalArgumentException(subTag + "buildCaptureRequest cameraInfo,map or size is null");
        }
        return null;
    }


    protected void sendCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice,CameraCaptureSession captureSession,CaptureRequest request) {
        if (cameraInfo == null || cameraDevice == null || captureSession == null || request == null){
            throw new IllegalArgumentException(subTag + "sendCaptureRequest cameraInfo,cameraDevice , captureSession or request is null");
        }
    }

    protected ImageReader buildImageReader(String tag, Size size, int format, int maxImages) {
        StringBuilder builder = new StringBuilder();
        builder.append("width=").append(size.getWidth()).append(" height=").append(size.getHeight()).append(" format=").append(format).append(" maxImages=").append(maxImages);
        Log.i(tag, subTag + "buildImageReader:" + builder.toString());
        ImageReader imageReader = ImageReader.newInstance(size.getWidth(), size.getHeight(), format, maxImages);
        imageReader.setOnImageAvailableListener(mImageAvailableListener, mCameraHandler);
        return imageReader;
    }

}
