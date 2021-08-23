package com.qiyei.android.media.lib;

import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.qiyei.android.media.api.CameraUtils;
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

    //触摸散光灯开关？
    private final boolean mIsTorchOn = false;

    //旋转角度？
    private int rotateDeg;

    private int cameraOrientation;

    private final MutableLiveData<Integer> mStatusLiveData = new MutableLiveData<>(HardwareStatus.DEFAULT);

    private int mCameraId;

    private WeakReference<SurfaceHolder> mSurfaceHolderRef;

    public Camera1Impl() {

    }

    @Override
    public boolean open() {
        return open(false);
    }

    @Override
    public boolean close() {
        stop();
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
        mStatusLiveData.postValue(HardwareStatus.CLOSE);
        return true;
    }

    @Override
    public boolean start() {
        return start(false);
    }

    @Override
    public boolean stop() {
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
        }
        mStatusLiveData.postValue(HardwareStatus.IDLE);
        return true;
    }

    @Override
    public void setCameraId(int id) {
        mCameraId = id;
    }

    @Override
    public LiveData<Integer> getStatusLiveData() {
        return mStatusLiveData;
    }

    @Override
    public void setPreviewOrientation(int orientation, int windowRotateDeg) {
        if (mCamera == null){
            throw new IllegalStateException("请调用open方法提前打开摄像头!");
        }

        mLastWindowRotateDeg = windowRotateDeg;
        mPreviewOrientation = orientation;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId,info);

        rotateDeg = windowRotateDeg;
        cameraOrientation = info.orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mPreviewRotation = info.orientation % 360;
                mPreviewRotation = (360 - mPreviewRotation) % 360;  // compensate the mirror
            } else {
                mPreviewRotation = (info.orientation + 360) % 360;
            }
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mPreviewRotation = (info.orientation - 90) % 360;
                mPreviewRotation = (360 - mPreviewRotation) % 360;  // compensate the mirror
            } else {
                mPreviewRotation = (info.orientation + 90) % 360;
            }
        }

        if (rotateDeg > 0) {
            mPreviewRotation = mPreviewRotation % rotateDeg;
        }

        mCamera.setDisplayOrientation(mPreviewRotation);
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
        mSurfaceHolderRef = surfaceHolderRef;
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


    private boolean open(boolean force){
        if (!force){
            if (isOpened()){
                Log.e(TAG, "摄像头 处于打开状态不允许重复打开!");
                return false;
            }
        }

        boolean result = false;

        try {
            mCamera = allocCamera();
            Camera camera = mCamera;
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPictureSize(mPreviewWidth,mPreviewHeight);
            int[] range = CameraUtils.determineMaximumSupportedFrameRate(parameters);
            Log.i(TAG,"range," + range[0] + " " + range[1]);
            parameters.setPreviewFpsRange(range[0],range[1]);
            parameters.setPictureFormat(ImageFormat.NV21);
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            parameters.setRecordingHint(true);

            //对焦模式
            List<String> supportedFocusModes = parameters.getSupportedFocusModes();
            if (supportedFocusModes != null && !supportedFocusModes.isEmpty()) {
                if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                } else {
                    parameters.setFocusMode(supportedFocusModes.get(0));
                }
            }

            List<String> supportedFlashModes = parameters.getSupportedFlashModes();
            if (supportedFlashModes != null && !supportedFlashModes.isEmpty()) {
                if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {

                    if (mIsTorchOn) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    }
                } else {
                    parameters.setFlashMode(supportedFlashModes.get(0));
                }
            }

            camera.setParameters(parameters);

            mStatusLiveData.setValue(HardwareStatus.OPEN);
            result = mCamera != null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private boolean isOpened(){
        return mStatusLiveData.getValue() == HardwareStatus.OPEN;
    }

    private boolean isRunning(){
        return mStatusLiveData.getValue() == HardwareStatus.RUNNING;
    }

    private boolean isClosed(){
        return mStatusLiveData.getValue() == HardwareStatus.CLOSE;
    }

    private boolean isIdle(){
        return mStatusLiveData.getValue() == HardwareStatus.IDLE;
    }


    /**
     * 分配Camera
     * @return
     */
    private Camera allocCamera() throws Exception{
        if (mCameraId < 0){
            mCameraId = getEnableCameraId();
        }

        Camera camera = Camera.open(mCameraId);
        camera.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera camera) {
                Log.e(TAG, "allocCamera: 错误回调状态码：" + error);
                close();
                open();
            }
        });
        return camera;
    }

    private int getEnableCameraId(){
        int id;
        Camera.CameraInfo info = new Camera.CameraInfo();
        int num = Camera.getNumberOfCameras();
        int frontId = -1;
        int backId = -1;
        for (int i = 0;i < num ;i++){
            Camera.getCameraInfo(i,info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
                backId = i;
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                frontId = i;
            }
        }

        if (frontId != -1){
            id = frontId;
        } else if (backId != -1){
            id = backId;
        } else {
            id = 0;
        }

        return id;
    }

    private boolean start(boolean force){
        if (!force) {
            if (isRunning()) {
                Log.e(TAG, "当前处于忙碌状态不允许重复预览!");
                return false;
            }
        }

        if (mCamera == null) {
            throw new IllegalStateException("请调用open方法提前打开摄像头!");
        }

        try {
            if (mSurfaceHolderRef != null){
                mCamera.setPreviewDisplay(mSurfaceHolderRef.get());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.startPreview();

        int previewFormat = mCamera.getParameters().getPreviewFormat();
        mPreviewSize = mCamera.getParameters().getPreviewSize();
        mCurrentPreviewCallbackBufferSize = mPreviewSize.width * mPreviewSize.height *3 / 2;

        int size = mPreviewSize.width * mPreviewSize.height * ImageFormat.getBitsPerPixel(previewFormat) >> 3;
        mCamera.addCallbackBuffer(new byte[size]);
        mCamera.setPreviewCallbackWithBuffer(mInnerPreviewCallback);

        mStatusLiveData.postValue(HardwareStatus.RUNNING);
        return true;
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
