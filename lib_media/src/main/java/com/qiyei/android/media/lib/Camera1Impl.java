package com.qiyei.android.media.lib;

import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.qiyei.android.media.api.HardwareStatus;
import com.qiyei.android.media.api.ICameraApi;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class Camera1Impl implements ICameraApi {


    private final static String TAG = "Camera1Impl";
    private Camera.PreviewCallback mPrevCb;
    protected Camera.Size mPreviewSize;
    private int mCurrentPreviewCallbackBufferSize = 0; // 理论回调缓冲区大小

    private final Camera.PreviewCallback mInnerPreviewCallback = (data, camera) -> {

        if (data.length != mCurrentPreviewCallbackBufferSize) {
            camera.addCallbackBuffer(data);
            return;
        }
        if (null != mPrevCb) mPrevCb.onPreviewFrame(data, camera);

        camera.addCallbackBuffer(data);
    };

    //默认的FPS
    private final int DEFAULT_FPS = 30;

    private Camera mCamera;

    private int mPreviewWidth = 640;
    private int mPreviewHeight = 480;

    private int mPreviewRotation = 90;
    private int mPreviewOrientation = Configuration.ORIENTATION_PORTRAIT;
    private int mLastWindowRotateDeg;
    private final boolean mIsTorchOn = false;

    //旋转角度？
    private int rotateDeg;

    private int cameraOrientation;

    private final MutableLiveData<Integer> mWorkingStatusLiveData = new MutableLiveData<>(HardwareStatus.DEFAULT);

    private int mCameraId;

    public Camera1Impl() {

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
        return mWorkingStatusLiveData;
    }

    @Override
    public void setPreviewOrientation(int orientation, int windowRotateDeg) {
        if (mCamera == null){
            throw new IllegalStateException("请调用open方法提前打开摄像头!");
        }

        Camera.Parameters parameters = mCamera.getParameters();
        mPreviewWidth =

    }

    @Override
    public int[] setPreviewResolution(int width, int height) {
        if (mCamera == null){
            throw new IllegalStateException("请调用open方法提前打开摄像头!");
        }

        Camera.Parameters parameters = mCamera.getParameters();
        mPreviewWidth = width;
        mPreviewHeight = height;

        Camera.Size size = getBestPreviewResolution(parameters, width, height);
        if (size != null){
            mPreviewWidth = size.width;
            mPreviewHeight = size.height;
        }

        parameters.setPictureSize(mPreviewWidth,mPreviewHeight);
        mCamera.setParameters(parameters);

        return new int[]{mPreviewWidth,mPreviewHeight};
    }

    @Override
    public void setSurfaceHolderRef(WeakReference<SurfaceHolder> surfaceHolderRef) {

    }

    @Override
    public void setPreviewCallback(Camera.PreviewCallback cb) {
        mPrevCb = cb;
    }

    @Override
    public MediaCodec getMediaCodec(String name, int encoderColorFormat) {
        MediaCodec mediaCodec = null;
        //RGB565格式
        int bitRate = 2 * mPreviewWidth * mPreviewHeight;

        try {
            mediaCodec = MediaCodec.createByCodecName(name);
            MediaFormat mediaFormat;
            if (rotateDeg == 0){
                mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,mPreviewHeight,mPreviewWidth);
            } else {
                mediaFormat =  MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mPreviewWidth, mPreviewHeight);
            }

            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,bitRate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,DEFAULT_FPS);
            //图片格式
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,encoderColorFormat);
            //I帧的间隔
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
            //编码
            mediaCodec.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);

            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mediaCodec;
    }

    @Override
    public int[] getPreviewResolution() {
        return new int[]{mPreviewWidth,mPreviewHeight};
    }

    @Override
    public int getCameraOrientation() {
        return cameraOrientation;
    }

    @Override
    public int getWindowDegree() {
        return rotateDeg;
    }

    @Override
    public void switchCamera() {
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        close();
        open(true);
        setPreviewOrientation(Configuration.ORIENTATION_PORTRAIT,mLastWindowRotateDeg);
        start(true);
    }

    @Override
    public void release() {

    }


    /**
     * 计算最佳的摄像头分辨率
     *
     * @param targetWidth
     * @param targetHeight
     * @return
     */
    private Camera.Size getBestPreviewResolution(@NonNull Camera.Parameters parameters, int targetWidth, int targetHeight) {
        if (mCamera == null)
            throw new IllegalStateException("请调用open方法提前打开摄像头!");
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();

        sortPreviewSizes(previewSizes, targetWidth, targetHeight);

        return previewSizes.get(0);
    }

    /**
     * 按照一定的条件对摄像头支持的分辨率列表进行排序
     *
     * @param previewSizes
     * @param desiredWidth
     * @param desiredHeight
     */
    private void sortPreviewSizes(List<Camera.Size> previewSizes, final int desiredWidth, final int desiredHeight) {

        final boolean landscape = true;
        Collections.sort(previewSizes, (o1, o2) -> {

            // 排序原理, 宽度高度比列最接近 16 : 9    尺寸最接近 1280 * 720 的
            float desiredScale = desiredHeight * 1.0f / desiredWidth;

            float scaleO1 = getSizeScale(o1);
            float scaleO2 = getSizeScale(o2);

            if (Math.abs(desiredScale - scaleO1) > Math.abs(desiredScale - scaleO2)) {
                return 1;
            } else if (Math.abs(desiredScale - scaleO1) < Math.abs(desiredScale - scaleO2)) {
                return -1;
            } else {
                // 比列相同, 寻找最 接近 1280 * 720 的尺寸
                int desired = landscape ? desiredWidth : desiredHeight;
                int o1Size = landscape ? o1.width : o1.height;
                int o2Size = landscape ? o2.width : o1.height;

                if (Math.abs(desired - o1Size) > Math.abs(desired - o2Size)) {
                    return 1;
                } else if (Math.abs(desired - o1Size) < Math.abs(desired - o2Size)) {
                    return -1;
                } else {
                    return Integer.compare(o2.width, o1.width);
                }
            }
        });
    }

    /**
     * 获取长宽比
     */
    private float getSizeScale(Camera.Size size) {
        return size.height * 1.0f / size.width;
    }
}
