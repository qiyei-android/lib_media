package com.qiyei.android.media.app.ui.view;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import com.qiyei.android.media.app.util.CameraHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Created by qiyei2015 on 2018/8/18.
 * @version: 1.0
 * @email: 1273482124@qq.com
 * @description: 摄像头预览View
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "CameraPreview";

    /**
     * 后置相机
     */
    public static final int CAMERA_FACING_BACK = 0;
    /**
     * 前置相机
     */
    public static final int CAMERA_FACING_FRONT = 1;

    /**
     * SurfaceHolder
     */
    private SurfaceHolder mSurfaceHolder;
    /**
     * 相机
     */
    private Camera mCamera;
    /**
     * 录制器
     */
    private MediaRecorder mMediaRecorder;

    /**
     * 拍照
     */
    public static final int MEDIA_TYPE_IMAGE = 1;
    /**
     * 视频
     */
    public static final int MEDIA_TYPE_VIDEO = 2;
    /**
     * 默认分辨率
     */
    private static int mOptVideoWidth = 1920;
    private static int mOptVideoHeight = 1080;

    /**
     * Context
     */
    private Context mContext;

    /**
     * 输出的uri
     */
    private Uri mOutputMediaFileUri;
    /**
     * 输出的文件类型
     */
    private String mOutputMediaFileType;
    /**
     * 角度
     */
    private int mDegree = 0;
    /**
     * 相机参数
     */
    private Camera.Parameters mParameters;


    public CameraPreview(Context context) {
        super(context);
        mContext = context;
        mSurfaceHolder = getHolder();
        //设置监听回调
        mSurfaceHolder.addCallback(this);
    }

    /**
     * 获取相机实例
     *
     * @return
     */
    private static Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open(CAMERA_FACING_BACK);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "getCameraInstance:" + e.toString());
        }
        return camera;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = getCameraInstance();
        //设置预览的holder
        try {
            mDegree = CameraHelper.getDisplayRotation(mContext);
            Log.i(TAG,"camera degree:" + mDegree);
            //设置相机预览角度
            CameraHelper.setCameraDisplayOrientation(CAMERA_FACING_BACK,mCamera,mDegree);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mCamera.autoFocus(new MyAutoFocusCallback());
            mCamera.cancelAutoFocus();
            getCameraOptimalVideoSize();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "surfaceCreated:" + e.toString());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged:format:" + format + " width:" + width + " height:" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    /**
     * 开始录制
     *
     * @return
     */
    public boolean startRecording() {
        if (prepareVideoRecorder()) {
            mMediaRecorder.start();
            return true;
        } else {
            releaseMediaRecorder();
        }
        return false;
    }

    /**
     * 停止录制
     */
    public void stopRecording() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
        }
        releaseMediaRecorder();
    }

    /**
     * 是否在录制
     *
     * @return
     */
    public boolean isRecording() {
        return mMediaRecorder != null;
    }

    /**
     * 获取输出文件的uri
     *
     * @return
     */
    public Uri getOutputMediaFileUri() {
        return mOutputMediaFileUri;
    }

    /**
     * 输出文件类型
     *
     * @return
     */
    public String getOutputMediaFileType() {
        return mOutputMediaFileType;
    }

    /**
     * 自动对焦回调
     */
    private class MyAutoFocusCallback implements Camera.AutoFocusCallback{

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success){
                camera.cancelAutoFocus();
                doAutoFocus();
            }
        }
    }

    /**
     * 获取可选的size
     */
    private void getCameraOptimalVideoSize() {
        Camera.Parameters parameters = mCamera.getParameters();
        try {
            //获取支持的分辨率
            List<Camera.Size> supportPreviewSizes = parameters.getSupportedPreviewSizes();
            //支持的视频分辨率
            List<Camera.Size> supportVideoSizes = parameters.getSupportedVideoSizes();
            Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(supportPreviewSizes, supportVideoSizes, getWidth(), getHeight());
            mOptVideoWidth = optimalSize.width;
            mOptVideoHeight = optimalSize.height;
            Log.i(TAG, "prepareVideoRecorder: optimalSize:" + mOptVideoWidth + ", " + mOptVideoHeight);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "getCameraOptimalVideoSize:" + e);
        }
    }

    /**
     * 准备录制器
     */
    private boolean prepareVideoRecorder() {
        if (mCamera == null) {
            mCamera = getCameraInstance();
        }
        mMediaRecorder = new MediaRecorder();
        //解锁camera，允许录制器使用
        mCamera.unlock();

        mMediaRecorder.setCamera(mCamera);
        //设置音频来源
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        //设置视频宽高
        //mMediaRecorder.setVideoSize(mOptVideoWidth,mOptVideoHeight);
        //设置视频录制时的角度
        mMediaRecorder.setOrientationHint(CameraHelper.getRecorderDegree(CAMERA_FACING_BACK,mDegree));
        //设置视频来源
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        //设置质量
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        //设置输出文件路径
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).getAbsolutePath());
        //设置预览surface
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "IOException:" + e);
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    /**
     * 释放录制器
     */
    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }

    /**
     * 获取输出文件路径
     *
     * @param type
     * @return
     */
    private File getOutputMediaFile(int type) {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), TAG);
        if (!mediaStorageDir.exists()) {
            //创建路径
            if (!mediaStorageDir.mkdirs()) {
                Log.i(TAG, "failed to create directory,use default storage");
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
        File mediaFile;
        switch (type) {
            case MEDIA_TYPE_IMAGE:
                mediaFile = new File(mediaStorageDir.getAbsolutePath() + File.separator + "IMG_" + timeStamp + ".jpg");
                mOutputMediaFileType = "image/*";
                break;
            case MEDIA_TYPE_VIDEO:
                mediaFile = new File(mediaStorageDir.getAbsolutePath() + File.separator + "VIDEO_" + timeStamp + ".mp4");
                mOutputMediaFileType = "video/*";
                break;
            default:
                mediaFile = null;
                break;
        }
        mOutputMediaFileUri = Uri.fromFile(mediaFile);
        Log.i(TAG, "mediaFile:" + mediaFile.getAbsolutePath());
        return mediaFile;
    }


    /**
     * 设置聚焦
     */
    private void doAutoFocus() {
        mParameters = mCamera.getParameters();
        mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mCamera.setParameters(mParameters);
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
                    mParameters = camera.getParameters();
                    mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    camera.setParameters(mParameters);
                }
            }
        });
    }
}
