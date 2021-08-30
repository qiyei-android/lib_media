package com.qiyei.android.media.lib.camera.camera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.qiyei.android.media.api.ICamera2Api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * 重构思路
 * 建立预览 拍照，视频录制等三个controller 分辨建立对应的surface  size imageListener
 */
public class Camera2Impl implements ICamera2Api,Handler.Callback {

    private static final String TAG = "Camera2Impl";
    private static final int TIME_OUT = 2000;

    private static final int MSG_OPEN_CAMERA = 0x100;
    private static final int MSG_CLOSE_CAMERA = 0x101;

    private static final int MSG_CREATE_REQUEST_BUILDERS = 0x200;

    private static final int MSG_CREATE_SESSION = 0x300;
    private static final int MSG_CLOSE_SESSION = 0x301;
    private static final int MSG_SET_PREVIEW_SIZE = 0x400;
    private static final int MSG_START_PREVIEW = 0x401;
    private static final int MSG_STOP_PREVIEW = 0x402;
    private static final int MSG_SET_IMAGE_SIZE = 0x500;
    private static final int MSG_CAPTURE_IMAGE = 0x501;
    private static final int MSG_CAPTURE_IMAGE_BURST = 0x502;
    private static final int MSG_START_CAPTURE_IMAGE_CONTINUOUSLY = 0x503;

    private static final int MSG_START_RECORD = 0x600;
    private static final int MSG_STOP_RECORD = 0x601;

    private Context mContext;

    /**
     * 相机管理器
     */
    private CameraManager mCameraManager;

    /**
     * Handler
     */
    private Handler mHandler;

    private Map<String, CameraInfo> mCameraMap;
    /**
     * 相机信息封装
     */
    private CameraInfo mCameraInfo;
    /**
     * 相机设备
     */
    private CameraDevice mCameraDevice;

    /**
     * 相机会话
     */
    private CameraCaptureSession mCaptureSession;

    private CameraCaptureSessionListener mSessionListener;

    private CameraPreviewController mCameraPreviewController;

    private CameraImageController mCameraImageController;

    private CameraRecordController mCameraRecordController;

    private CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "deviceStateCallback onOpened: " + camera);
            mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.i(TAG, "deviceStateCallback onDisconnected: " + camera);
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "StateCallback onError: " + error);
            camera.close();
            mCameraDevice = null;

            sendMessage(MSG_STOP_PREVIEW,null);
        }
    };

    CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.i(TAG, "sessionStateCallback ,onConfigured");
            if (mCameraDevice == null) {
                return;
            }
            mCaptureSession = session;
            if (mSessionListener != null){
                mSessionListener.onAvailable();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "sessionStateCallback onConfigureFailed");
        }
    };

    public Camera2Impl(Context context) {
        mContext = context;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        mHandler = new Handler(Looper.getMainLooper(),this);

        mCameraMap = new HashMap<>();

        mCameraPreviewController = new CameraPreviewController(mContext,TAG);
        mCameraImageController = new CameraImageController(mContext,TAG);
        mCameraRecordController = new CameraRecordController(mContext,TAG);

        initCamera();
    }

    @Override
    public void setSessionListener(CameraCaptureSessionListener sessionListener) {
        mSessionListener = sessionListener;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        int what = msg.what;
        Log.i(TAG,"handleMessage what=0x" + String.format("%03x",what) + " start");
        switch (what){
            case MSG_OPEN_CAMERA:
                String cameraId = null;
                if (msg.obj instanceof String){
                    cameraId = (String) msg.obj;
                }
                boolean ret = open(cameraId);
                Log.i(TAG,"handleMessage open=" + ret);
                break;
            case MSG_CLOSE_CAMERA:
                closeInner();
                break;
            case MSG_CREATE_REQUEST_BUILDERS:
                if (mCameraDevice == null){
                    Log.e(TAG, "startPreview error,camera device is null");
                    break;
                }

                break;
            case MSG_CREATE_SESSION:
                createSessionInner();
                break;
            case MSG_CLOSE_SESSION:
                closeSession();
                break;
            case MSG_SET_PREVIEW_SIZE:
                Size size = (Size) msg.obj;
                mCameraPreviewController.setSurfaceSize(mCameraInfo,size);
                break;
            case MSG_START_PREVIEW:
                startPreviewInner();
                break;
            case MSG_STOP_PREVIEW:
                stopPreviewInner();
                break;
            case MSG_SET_IMAGE_SIZE:
                Size imageSize = (Size) msg.obj;
                mCameraImageController.setSurfaceSize(mCameraInfo,imageSize);
                break;
            case MSG_CAPTURE_IMAGE:
                startImageCapture();
                break;
            case MSG_CAPTURE_IMAGE_BURST:

                break;
            case MSG_START_CAPTURE_IMAGE_CONTINUOUSLY:

                break;
            case MSG_START_RECORD:

                break;
            case MSG_STOP_RECORD:

                break;
            default:
        }


        return false;
    }

    @Override
    public boolean open() {
        return open(mCameraInfo.cameraId);
    }

    @Override
    public void close() {
        sendMessage(MSG_CLOSE_CAMERA,null);
    }

    @Override
    public void setPreviewSurfaceTexture(SurfaceTexture texture) {
        mCameraPreviewController.setPreviewSurfaceTexture(texture);
    }

    @Override
    public void setPreviewSize(Size size) {
        sendMessage(MSG_SET_PREVIEW_SIZE,size);
    }

    @Override
    public void setImageSize(Size size) {
        sendMessage(MSG_SET_IMAGE_SIZE,size);
    }

    @Override
    public void createSession() {
        sendMessage(MSG_CREATE_SESSION,null);
    }

    @Override
    public void startPreview() {
        sendMessage(MSG_START_PREVIEW,null,0);
    }

    @Override
    public void stopPreview() {
        sendMessage(MSG_STOP_PREVIEW,null);
    }

    @Override
    public void takePhoto() {
        sendMessage(MSG_CAPTURE_IMAGE,null);
    }

    @Override
    public void setRecordSize(Size size) {
        mCameraRecordController.setSurfaceSize(mCameraInfo,size);
    }

    @Override
    public void startRecord() {
        if (mCameraDevice == null){
            Log.e(TAG,"startPreview error, cameraDevice is null");
            return;
        }

        if (mCaptureSession == null){
            Log.e(TAG,"startPreview error, captureSession is null");
            return;
        }
        mCameraRecordController.sendCaptureRequest(mCameraInfo,mCameraDevice,mCaptureSession);
    }

    private boolean initCamera() {
        mCameraMap.clear();

        try {
            String[] ids = mCameraManager.getCameraIdList();
            if (ids == null || ids.length == 0) {
                Log.e(TAG, "initCamera error,No available camera.");
                return false;
            }

            for (String cameraId : ids) {
                //获取相机信息
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);

                //获取流配置map
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                //厂商支持级别
                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                Log.i(TAG, "initCamera,id= " + cameraId + " support level = " + level);
                if (level == null || CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY == level) {
                    continue;
                }

                //获取相机face
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null) {
                    continue;
                }

                CameraInfo info = new CameraInfo();
                info.cameraId = cameraId;
                info.characteristics = characteristics;
                info.level = level;
                info.facing = facing;
                info.map = map;

                mCameraMap.put(cameraId, info);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        StringBuilder builder = new StringBuilder();
        mCameraMap.forEach(new BiConsumer<String, CameraInfo>() {
            @Override
            public void accept(String id, CameraInfo cameraInfo) {
                if (cameraInfo.facing == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraInfo = cameraInfo;
                }
                builder.append(id).append("-").append(cameraInfo.toString()).append("\n");
            }
        });

        if (builder.length() != 0) {
            builder.delete(builder.length() - 1, builder.length());
        }

        Log.i(TAG, "initCamera list=" + builder.toString());
        if (mCameraInfo == null) {
            mCameraInfo = mCameraMap.get(CameraCharacteristics.LENS_FACING_BACK);
        }

        //初始化surface


        if (mCameraMap.size() != 0 && mCameraInfo != null) {
            return true;
        } else {
            return false;
        }
    }

    private boolean open(String cameraId) {
        mCameraInfo = mCameraMap.get(cameraId);

        if (mCameraInfo == null) {
            Log.e(TAG, "Open camera failed. No camera available cameraId=" + cameraId);
            return false;
        }
        try {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Open camera failed. No camera permission");
                return false;
            }
            mCameraManager.openCamera(cameraId, mDeviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "open error:" + e.getMessage());
            return false;
        }
        return true;
    }

    private void closeInner(){
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        Log.e(TAG, "camera close ");
    }

    private void createSessionInner(){
        if (mCameraInfo == null){
            Log.e(TAG, "createSession error cameraInfo is null");
            return;
        }
        if (mCameraDevice == null){
            Log.e(TAG, "createSession error mCameraDevice is null");
            return;
        }
        Range<Integer>[] fpsRanges = mCameraInfo.characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        Log.i(TAG, "createSessionInner fpsRanges=" + Arrays.toString(fpsRanges));

        List<Surface> surfaceList = new ArrayList<>();

        surfaceList.addAll(mCameraPreviewController.getSurfaces());
        surfaceList.addAll(mCameraImageController.getSurfaces());
        surfaceList.addAll(mCameraRecordController.getSurfaces());

        try {
            Log.i(TAG, "createSessionInner surfaceList.size=" + surfaceList.size());
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                List<OutputConfiguration> list = new ArrayList<>();
                surfaceList.forEach(new Consumer<Surface>() {
                    @Override
                    public void accept(Surface surface) {
                        list.add(new OutputConfiguration(surface));
                    }
                });

                SessionConfiguration configuration = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                        list, Executors.newSingleThreadExecutor(), mSessionStateCallback);
                mCameraDevice.createCaptureSession(configuration);
            } else {
                mCameraDevice.createCaptureSession(surfaceList, mSessionStateCallback, mHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeSession(){
        if (mCaptureSession != null){
            mCaptureSession.close();
            mCaptureSession = null;
        }
        Log.i(TAG, "closeSession ");
    }

    private void startPreviewInner(){
        if (mCameraDevice == null){
            Log.e(TAG,"startPreview error, cameraDevice is null");
            return;
        }

        if (mCaptureSession == null){
            Log.e(TAG,"startPreview error, captureSession is null");
            return;
        }
        mCameraPreviewController.sendCaptureRequest(mCameraInfo,mCameraDevice,mCaptureSession);
    }

    private void stopPreviewInner(){
        if (mCameraDevice == null){
            Log.e(TAG,"stopPreviewInner error, cameraDevice is null");
            return;
        }

        if (mCaptureSession == null){
            Log.e(TAG,"stopPreviewInner error, captureSession is null");
            return;
        }

        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startImageCapture(){
        if (mCameraDevice == null){
            Log.e(TAG,"startImageCapture error, cameraDevice is null");
            return;
        }

        if (mCaptureSession == null){
            Log.e(TAG,"startImageCapture error, captureSession is null");
            return;
        }
        mCameraImageController.sendCaptureRequest(mCameraInfo,mCameraDevice,mCaptureSession);
    }


    private void sendMessage(int what,Object obj){
        sendMessage(what,obj,0);
    }

    private void sendMessage(int what,Object obj,long delay){
        Message msg = mHandler.obtainMessage(what);
        msg.obj = obj;
        mHandler.sendMessageDelayed(msg,delay);
        Log.i(TAG,"sendMessage what=0x" + String.format("%03x",what));
    }
}
