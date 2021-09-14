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
import android.media.Image;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class Camera2Impl implements ICamera2Api {

    private static final String TAG = "Camera2Impl";

    /**
     * context
     */
    private Context mContext;

    /**
     * 相机管理器
     */
    private CameraManager mCameraManager;

    /**
     * Handler
     */
    private Handler mHandler;

    /**
     * 相机列表
     */
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

    /**
     * 相机监听器
     */
    private CameraListener mCameraListener;

    private CameraPreviewController mCameraPreviewController;

    private CameraImageController mCameraImageController;

    private CameraRecordController mCameraRecordController;

    private Queue<Runnable> mTaskQueue = new LinkedList<>();

    private boolean hasCreateSession;

    private CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "deviceStateCallback onOpened: " + camera);
            mCameraDevice = camera;
            if (mCameraListener != null){
                mCameraListener.onDeviceAvailable();
            }
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

            while (!mTaskQueue.isEmpty()){
                Runnable task = mTaskQueue.poll();
                if (task != null){
                    task.run();
                }
            }
            if (mCameraListener != null){
                mCameraListener.onSessionAvailable();
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

        mHandler = new Handler(Looper.getMainLooper());

        mCameraMap = new HashMap<>();

        initCamera();
    }

    @Override
    public void init(int type) {
        mCameraPreviewController = new CameraPreviewController(mContext,TAG){
            @Override
            public void onImageCallBack(Image image) {
                if (mCameraListener != null){
                    mCameraListener.onImageAvailable(image);
                }
            }
        };
        //mCameraImageController = new CameraImageController(mContext,TAG);
        //mCameraRecordController = new CameraRecordController(mContext,TAG);
    }

    @Override
    public boolean open(CameraListener listener) {
        if (listener == null){
            return false;
        }
        mCameraListener = listener;
        boolean ret = open(mCameraInfo.cameraId);
        if (mCameraPreviewController != null){
            mCameraPreviewController.open();
        }
        if (mCameraImageController != null){
            mCameraImageController.open();
        }
        if (mCameraRecordController != null){
            mCameraRecordController.open();
        }
        return ret;
    }

    @Override
    public void close() {
        closeSession();
        closeDevice();
        Log.i(TAG, "camera close ");
    }


    @Override
    public void setPreviewSurfaceTexture(SurfaceTexture texture) {
        if (mCameraPreviewController != null){
            mCameraPreviewController.setPreviewSurfaceTexture(texture);
        }
    }

    @Override
    public void setPreviewSize(Size size) {
        if (mCameraPreviewController != null){
            mCameraPreviewController.setSurfaceSize(mCameraInfo,size);
        }
    }

    @Override
    public void setImageSize(Size size) {
        if (mCameraImageController != null){
            mCameraImageController.setSurfaceSize(mCameraInfo,size);
        }
    }

    @Override
    public void setRecordSize(Size size) {
        if (mCameraRecordController != null){
            mCameraRecordController.setSurfaceSize(mCameraInfo,size);
        }
    }

    @Override
    public void start() {
        if (mCameraPreviewController != null){
            mCameraPreviewController.prepare(mCameraInfo);
        }
        if (mCameraImageController != null){
            mCameraImageController.prepare(mCameraInfo);
        }
        if (mCameraRecordController != null){
            mCameraRecordController.prepare(mCameraInfo);
        }
        if (!hasCreateSession){
            createSession();
        }
    }


    @Override
    public void stop() {
        if (mCameraPreviewController != null){
            mCameraPreviewController.stop();
        }
        if (mCameraImageController != null){
            mCameraImageController.stop();
        }
        if (mCameraRecordController != null){
            mCameraRecordController.stop();
        }
        closeSession();
    }


    @Override
    public void startPreview() {
        if (mCameraDevice == null){
            Log.e(TAG,"startPreview error, cameraDevice is null");
            return;
        }
        sendTask(new Runnable() {
            @Override
            public void run() {
                if (mCameraPreviewController != null){
                    mCameraPreviewController.start(mCameraInfo,mCameraDevice,mCaptureSession);
                }
            }
        });
    }

    @Override
    public void stopPreview() {
        if (mCameraDevice == null){
            Log.e(TAG,"stopPreviewInner error, cameraDevice is null");
            return;
        }

        if (mCaptureSession == null){
            Log.e(TAG,"stopPreviewInner error, captureSession is null");
            return;
        }

        if (mCameraPreviewController != null){
            mCameraPreviewController.stop();
        }

        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        closeSession();
    }

    @Override
    public void takePhoto() {
        if (mCameraDevice == null){
            Log.e(TAG,"startImageCapture error, cameraDevice is null");
            return;
        }
        sendTask(new Runnable() {
            @Override
            public void run() {
                if (mCameraImageController != null){
                    mCameraImageController.start(mCameraInfo,mCameraDevice,mCaptureSession);
                }
            }
        });
    }

    @Override
    public void startRecord() {
        if (mCameraDevice == null){
            Log.e(TAG,"startPreview error, cameraDevice is null");
            return;
        }
        sendTask(new Runnable() {
            @Override
            public void run() {
                if (mCameraRecordController != null){
                    mCameraRecordController.start(mCameraInfo,mCameraDevice,mCaptureSession);
                }
            }
        });
    }

    @Override
    public void stopRecord() {
        mCameraRecordController.stop();
        closeSession();
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

    private void createSession() {
        if (mCameraInfo == null){
            Log.e(TAG, "createSession error cameraInfo is null");
            return;
        }
        if (mCameraDevice == null){
            Log.e(TAG, "createSession error CameraDevice is null");
            return;
        }
        Range<Integer>[] fpsRanges = mCameraInfo.characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        Log.i(TAG, "createSessionInner fpsRanges=" + Arrays.toString(fpsRanges));

        List<Surface> surfaceList = new ArrayList<>();

        if (mCameraPreviewController != null){
            surfaceList.addAll(mCameraPreviewController.getSurfaces(mCameraInfo));
        }
        if (mCameraImageController != null){
            surfaceList.addAll(mCameraImageController.getSurfaces(mCameraInfo));
        }
        if (mCameraRecordController != null){
            surfaceList.addAll(mCameraRecordController.getSurfaces(mCameraInfo));
        }

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
            hasCreateSession = true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void sendTask(Runnable task){
        if (!hasCreateSession || mCaptureSession == null){
            createSession();
            mTaskQueue.add(task);
        } else {
            task.run();
        }
    }

    private void closeDevice() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void closeSession(){
        hasCreateSession = false;
        if (mCaptureSession != null){
            mCaptureSession.close();
            mCaptureSession = null;
        }
        Log.i(TAG, "closeSession ");
    }
}
