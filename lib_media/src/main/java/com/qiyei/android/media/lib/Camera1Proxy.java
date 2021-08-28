package com.qiyei.android.media.lib;

import android.hardware.Camera;
import android.media.MediaCodec;
import android.view.SurfaceHolder;

import androidx.lifecycle.LiveData;

import com.qiyei.android.media.api.ICamera1Api;

import java.lang.ref.WeakReference;

public class Camera1Proxy implements ICamera1Api {

    private volatile static Camera1Proxy sInstance;

    private Camera1Impl mCameraImpl;


    private Camera1Proxy() {
        mCameraImpl = new Camera1Impl();
    }


    public static Camera1Proxy getInstance(){
        if (sInstance == null){
            synchronized (Camera1Proxy.class){
                if (sInstance == null){
                    sInstance = new Camera1Proxy();
                }
            }
        }
        return sInstance;
    }


    @Override
    public boolean open() {
        return mCameraImpl.open();
    }

    @Override
    public boolean close() {
        return mCameraImpl.close();
    }

    @Override
    public boolean start() {
        return mCameraImpl.start();
    }

    @Override
    public boolean stop() {
        return mCameraImpl.stop();
    }

    @Override
    public void setCameraId(int id) {
        mCameraImpl.setCameraId(id);
    }

    @Override
    public LiveData<Integer> getStatusLiveData() {
        return mCameraImpl.getStatusLiveData();
    }

    @Override
    public void setPreviewOrientation(int orientation, int windowRotateDeg) {
        mCameraImpl.setPreviewOrientation(orientation,windowRotateDeg);
    }

    @Override
    public int[] setPreviewResolution(int width, int height) {
        return mCameraImpl.setPreviewResolution(width,height);
    }

    @Override
    public void setSurfaceHolderRef(WeakReference<SurfaceHolder> surfaceHolderRef) {
        mCameraImpl.setSurfaceHolderRef(surfaceHolderRef);
    }

    @Override
    public void setPreviewCallback(Camera.PreviewCallback cb) {
        mCameraImpl.setPreviewCallback(cb);
    }

    @Override
    public MediaCodec getMediaCodec(String name, int encoderColorFormat) {
        return mCameraImpl.getMediaCodec(name,encoderColorFormat);
    }

    @Override
    public int[] getPreviewResolution() {
        return mCameraImpl.getPreviewResolution();
    }

    @Override
    public int getCameraOrientation() {
        return mCameraImpl.getCameraOrientation();
    }

    @Override
    public int getWindowDegree() {
        return mCameraImpl.getWindowDegree();
    }

    @Override
    public void switchCamera() {
        mCameraImpl.switchCamera();
    }
}
