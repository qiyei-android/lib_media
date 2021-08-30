package com.qiyei.android.media.lib.camera.camera2;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.qiyei.android.media.api.CameraUtils;

import java.util.ArrayList;
import java.util.List;

public class CameraPreviewController extends AbsCameraController{

    /**
     * 预览SurfaceTexture
     */
    private SurfaceTexture mPreviewSurfaceTexture;

    /**
     * 预览Surface
     */
    private Surface mPreviewSurface;

    public CameraPreviewController(Context context,String tag) {
        super(context,tag,"CameraPreview");
    }

    public void setPreviewSurfaceTexture(SurfaceTexture texture){
        this.mPreviewSurfaceTexture = texture;
    }

    @Override
    public void onImageCallBack(Image image) {
        Log.i(mTag, subTag + "##### ImageAvailableListener onImageAvailable,planes=" + image.getPlanes());
    }

    @Override
    public void setSurfaceSize(CameraInfo cameraInfo, Size size) {
        super.setSurfaceSize(cameraInfo,size);
        mSurfaceSize = size;
        Size optimalSize = CameraUtils.getOptimalSize(cameraInfo.map, SurfaceTexture.class,size.getWidth(),size.getHeight());
        if (optimalSize != null){
            mSurfaceSize = optimalSize;
        }
        if (mPreviewSurfaceTexture != null){
            mPreviewSurfaceTexture.setDefaultBufferSize(mSurfaceSize.getWidth(), mSurfaceSize.getHeight());
            mPreviewSurface = new Surface(mPreviewSurfaceTexture);
        }
        mImageReader = buildImageLoader(mTag,mSurfaceSize,ImageFormat.YUV_420_888,1);
    }

    @Override
    public List<Surface> getSurfaces() {
        List<Surface> list = new ArrayList<>();
        list.addAll(super.getSurfaces());
        if (mPreviewSurface != null){
            list.add(mPreviewSurface);
        }
        return list;
    }

    @Override
    public CaptureRequest buildCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice) {
        super.buildCaptureRequest(cameraInfo,cameraDevice);
        try {
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            for (Surface surface : getSurfaces()){
                mCaptureRequestBuilder.addTarget(surface);
            }

            return mCaptureRequestBuilder.build();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void sendCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice,CameraCaptureSession captureSession) {
        super.sendCaptureRequest(cameraInfo,cameraDevice,captureSession);

        CaptureRequest request = buildCaptureRequest(cameraInfo,cameraDevice);
        if (request == null){
            Log.e(mTag,subTag + "sendCaptureRequest error, CaptureRequest is null");
            return;
        }
        try {
            captureSession.setRepeatingRequest(request, new CameraCaptureSession.CaptureCallback() {
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
            },getHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
