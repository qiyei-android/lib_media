package com.qiyei.android.media.lib;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.view.SurfaceHolder;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;
import com.qiyei.android.media.api.ICamera1Api;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

public class CameraXImpl implements ICamera1Api {

    private Context mContext;

    private ImageCapture mImageCapture = null;

    PreviewView viewFinder;

    public CameraXImpl(Context context, LifecycleOwner lifecycleOwner) {
        mContext = context;

        //获取相机实例
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        //添加监听
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    //获取具体的相机Provider
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    //新建预览
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                    //选着相机
                    CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;


                    //全部解绑
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(lifecycleOwner,selector,preview);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(mContext));

    }

    private void startPreview(){

    }

    private void startImageCapture(){

    }

    private void startImageAnalysis(){

    }


    @Override
    public boolean open() {
        return false;
    }

    @Override
    public boolean close() {
        return false;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public void setCameraId(int id) {

    }

    @Override
    public LiveData<Integer> getStatusLiveData() {
        return null;
    }

    @Override
    public void setPreviewOrientation(int orientation, int windowRotateDeg) {

    }

    @Override
    public int[] setPreviewResolution(int width, int height) {
        return new int[0];
    }

    @Override
    public void setSurfaceHolderRef(WeakReference<SurfaceHolder> surfaceHolderRef) {

    }

    @Override
    public void setPreviewCallback(Camera.PreviewCallback cb) {

    }

    @Override
    public MediaCodec getMediaCodec(String name, int encoderColorFormat) {
        return null;
    }

    @Override
    public int[] getPreviewResolution() {
        return new int[0];
    }

    @Override
    public int getCameraOrientation() {
        return 0;
    }

    @Override
    public int getWindowDegree() {
        return 0;
    }

    @Override
    public void switchCamera() {

    }
}
