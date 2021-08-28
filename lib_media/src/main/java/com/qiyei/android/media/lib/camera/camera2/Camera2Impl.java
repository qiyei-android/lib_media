package com.qiyei.android.media.lib.camera.camera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.qiyei.android.media.api.CameraUtils;
import com.qiyei.android.media.api.ICamera2Api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Camera2Impl implements ICamera2Api,Handler.Callback {

    private static final String TAG = "Camera2Impl";
    private static final int TIME_OUT = 2000;

    private CameraManager mCameraManager;

    /**
     * 相机线程
     */
    private HandlerThread mCameraThread;
    /**
     * 相机Handler
     */
    private Handler mCameraHandler;

    private Context mContext;

    private Map<String, CameraInfo> mCameraMap;

    private CameraInfo mCameraInfo;

    private CameraDevice mCameraDevice;

    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest.Builder mPreviewRequestBuilder;

    private CameraCaptureSession mCaptureSession;

    private ImageReader mImageReader;

    List<Surface> mSurfaces = new ArrayList<>();


    private SurfaceTexture mPreviewSurfaceTexture;

    private Surface mPreviewSurface;

    //private Surface mImageSurface;

    private Size mDefaultPreviewSize = new Size(640,960);

    private Size mDefaultImageSize = new Size(1080,1920);


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


    private CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "deviceStateCallback onOpened: " + camera);
            mCameraDevice = camera;

            //创建request
            sendMessage(MSG_CREATE_REQUEST_BUILDERS,null);
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            //mCameraLock.release();
            Log.i(TAG, "deviceStateCallback onDisconnected: " + camera);
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "StateCallback onError: " + error);
            //mCameraLock.release();
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
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "sessionStateCallback onConfigureFailed");
        }
    };

    private ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
//            mBuffer = convertYUV420888ToNv21(image);
//            if (mFrameCallback != null) {
            Log.i(TAG, "##### mImageAvailableListener onImageAvailable: " + image.getPlanes());
//                mFrameCallback.onFrame(image);
//            }
            image.close();
        }
    };


    public Camera2Impl(Context context) {
        mContext = context;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper(),this);

        mCameraMap = new HashMap<>();
        initCamera();
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
                try {
                    mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
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
                setPreviewSize(size.getWidth(),size.getHeight());
                break;
            case MSG_START_PREVIEW:
                startPreviewInner();
                break;
            case MSG_STOP_PREVIEW:
                stopPreviewInner();
                break;
            case MSG_SET_IMAGE_SIZE:
                Size imageSize = (Size) msg.obj;
                int format = ImageFormat.YUV_420_888;
                int maxImages = 1;
                setImageSize(imageSize.getWidth(),imageSize.getHeight(),format,maxImages);
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
        mPreviewSurfaceTexture = texture;
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
        sendMessage(MSG_START_PREVIEW,null,1000);
    }

    @Override
    public void stopPreview() {
        sendMessage(MSG_STOP_PREVIEW,null);
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
            mCameraManager.openCamera(cameraId, mDeviceStateCallback, mCameraHandler);
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
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        Log.e(TAG, "camera close ");
    }

    private void createSessionInner(){
        if (mCameraInfo == null){
            Log.e(TAG, "createSession error cameraInfo is null");
            return;
        }
        Range<Integer>[] fpsRanges = mCameraInfo.characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        Log.i(TAG, "createSessionInner fpsRanges=" + Arrays.toString(fpsRanges));

        List<Surface> surfaceList = new ArrayList<>();
        if (mImageReader != null){
            surfaceList.add(mImageReader.getSurface());
        }

        if (mPreviewSurface != null){
            surfaceList.add(mPreviewSurface);
        }

        if (mSurfaces != null && mSurfaces.size() != 0){
            surfaceList.addAll(mSurfaces);
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
                mCameraDevice.createCaptureSession(surfaceList, mSessionStateCallback,mCameraHandler);
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

    private void setPreviewSize(int maxWidth,int maxHeight){
        if (mCameraInfo != null){
            Size previewSize = CameraUtils.getOptimalSize(mCameraInfo.map,SurfaceTexture.class,maxWidth,maxHeight);
            if (previewSize == null){
                previewSize = mDefaultPreviewSize;
            }
            if (mPreviewSurfaceTexture != null){
                mPreviewSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                mPreviewSurface = new Surface(mPreviewSurfaceTexture);
            }
        }
    }

    private void setImageSize(int width,int height,Integer format,int maxImages){
        if (format == null){
            //camera2 默认是YUV_420_888
            format = ImageFormat.YUV_420_888;
        }

        if (mCameraInfo.map.isOutputSupportedFor(format)){
            mImageReader = buildImageLoader(width,height,format,maxImages);
        } else {
            Log.w(TAG,"mCameraInfo id=" + mCameraInfo.cameraId + " not support format=" + format);
        }
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

        if (mCaptureRequestBuilder == null || mPreviewRequestBuilder == null){
            Log.e(TAG,"startPreview error, captureRequestBuilder =" + mCaptureRequestBuilder + " or previewRequestBuilder =" +mPreviewRequestBuilder);
            return;
        }

        if (mPreviewSurface != null){
            mCaptureRequestBuilder.addTarget(mPreviewSurface);
            mPreviewRequestBuilder.addTarget(mPreviewSurface);
        }

        if (mImageReader != null){
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
        }

        try {
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                    super.onCaptureProgressed(session, request, partialResult);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }
            },mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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

        if (mCaptureRequestBuilder == null){
            Log.e(TAG,"startImageCapture error, captureRequestBuilder is null");
            return;
        }

//        val deviceOrientation = deviceOrientationListener.orientation;
//        val jpegOrientation = getJpegOrientation(cameraCharacteristics, deviceOrientation);
//        captureImageRequestBuilder[CaptureRequest.JPEG_ORIENTATION] = jpegOrientation;
//
//        // Configure the location information.
//        val location = getLocation();
//        captureImageRequestBuilder[CaptureRequest.JPEG_GPS_LOCATION] = location;
//
//        // Configure the image quality.
//        captureImageRequestBuilder[CaptureRequest.JPEG_QUALITY] = 100;
//
//        // Add the target surface to receive the jpeg image data.
//        captureImageRequestBuilder.addTarget(jpegSurface);
//
//        val captureImageRequest = captureImageRequestBuilder.build();
//        captureSession.capture(captureImageRequest, CaptureImageStateCallback(), mainHandler);
    }


    private void sendMessage(int what,Object obj){
        sendMessage(what,obj,0);
    }

    private void sendMessage(int what,Object obj,long delay){
        Message msg = mCameraHandler.obtainMessage(what);
        msg.obj = obj;
        mCameraHandler.sendMessageDelayed(msg,delay);
        Log.i(TAG,"sendMessage what=0x" + String.format("%03x",what));
    }

    private ImageReader buildImageLoader(int width,int height,int format,int maxImages) {
        StringBuilder builder = new StringBuilder();
        builder.append("width=").append(width).append(" height=").append(height).append(" format=").append(format).append(" maxImages=").append(maxImages);
        Log.i(TAG, "buildImageLoader:" + builder.toString());
        ImageReader imageReader = ImageReader.newInstance(width, height, format, maxImages);
        imageReader.setOnImageAvailableListener(mImageAvailableListener, mCameraHandler);
        return imageReader;
    }
}
