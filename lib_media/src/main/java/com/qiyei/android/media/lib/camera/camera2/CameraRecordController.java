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
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.qiyei.android.media.api.CameraUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class CameraRecordController extends AbsCameraController{

    private static final int MAX_BIT_RATE = 10 * 1000 * 1000;

    private static final int FRAME_RATE = 60;

    /**
     * 快门声音
     */
    private MediaActionSound mMediaActionSound;

    /**
     * 设备方向监听
     */
    private DeviceOrientationListener mDeviceOrientationListener;


    private MediaRecorder mMediaRecorder;

    private String mVideoPath;

    private boolean isPrepare;

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
    public void prepare(CameraInfo cameraInfo) {
        mDeviceOrientationListener.enable();
        mMediaRecorder = new MediaRecorder();
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            mVideoPath = getVideoPath();
            mMediaRecorder.setOutputFile(mVideoPath);

            mMediaRecorder.setVideoEncodingBitRate(MAX_BIT_RATE);
            mMediaRecorder.setVideoFrameRate(FRAME_RATE);
            //获取可用的录制视频的尺寸
            mMediaRecorder.setVideoSize(mSurfaceSize.getWidth(), mSurfaceSize.getHeight());

            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            int deviceOrientation = mDeviceOrientationListener.getOrientation();
            mMediaRecorder.setOrientationHint(CameraUtils.getOrientation(cameraInfo.characteristics,deviceOrientation));

            mMediaRecorder.prepare();
            Log.i(mTag,subTag + "prepare,cameraInfo.cameraId=" + cameraInfo.cameraId);
            isPrepare = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(CameraInfo cameraInfo, CameraDevice cameraDevice, CameraCaptureSession captureSession) {
        //2 创建请求
        CaptureRequest request = buildCaptureRequest(cameraInfo,cameraDevice);
        if (request == null){
            Log.e(mTag,subTag + "start error, CaptureRequest is null");
            return;
        }
        //3 开始会话
        sendCaptureRequest(cameraInfo,cameraDevice,captureSession,request);
    }

    @Override
    public void stop() {
        super.stop();
        mDeviceOrientationListener.disable();
        mMediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING);

        mMediaRecorder.stop();
        mMediaRecorder.reset();
        isPrepare = false;
    }


    @Override
    public void onImageCallBack(Image image) {
        Log.i(mTag, subTag + "##### ImageAvailableListener onImageAvailable,planes=" + image.getPlanes());

    }

    @Override
    public void setSurfaceSize(CameraInfo cameraInfo, Size size) {
        super.setSurfaceSize(cameraInfo,size);
        Size optimalSize = CameraUtils.getOptimalSize(cameraInfo.map,MediaRecorder.class,size.getWidth(),size.getHeight());
        if (optimalSize != null){
            mSurfaceSize = optimalSize;
        }
        Log.i(mTag,subTag + "setSurfaceSize,optimalSize=" + (optimalSize== null ? null : optimalSize.toString()) + mSurfaceSize.toString());
//        if (cameraInfo.map.isOutputSupportedFor(ImageFormat.YUV_420_888)){
//            mImageReader = buildImageReader(mTag, mSurfaceSize,ImageFormat.YUV_420_888,1);
//        } else {
//            Log.w(mTag,subTag + "mCameraInfo id=" + cameraInfo.cameraId + " not support format=" + ImageFormat.YUV_420_888);
//        }
    }


    @Override
    public CaptureRequest buildCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice) {
        super.buildCaptureRequest(cameraInfo,cameraDevice);
        try {
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            for (Surface surface : getSurfaces(cameraInfo)){
                mCaptureRequestBuilder.addTarget(surface);
            }
            return mCaptureRequestBuilder.build();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
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
            if (!isPrepare){
                prepare(cameraInfo);
            }
            if (mMediaRecorder != null){
                mSurfaces.add(mMediaRecorder.getSurface());
            }
        }
        Log.i(mTag,subTag + "getSurfaces isPrepare=" + isPrepare);
        return mSurfaces;
    }

    @Override
    protected void sendCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice,CameraCaptureSession captureSession,CaptureRequest request) {
        super.sendCaptureRequest(cameraInfo,cameraDevice,captureSession,request);
        mMediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING);
        //开始录制
        mMediaRecorder.start();
        try {
            captureSession.setRepeatingRequest(request, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);

                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }
            }, getHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private String getVideoPath(){
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());

        String cameraDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator +  "Camera";

        long date = System.currentTimeMillis();
        //e.g. IMG_20190211100833786
        String title = "video_" + dateFormat.format(date);
        // e.g. IMG_20190211100833786.jpeg
        String displayName = title + ".mp4";;//
        ///sdcard/DCIM/Camera/video_20190211100833786.mp4
        return cameraDir + File.separator + displayName;
    }
}
