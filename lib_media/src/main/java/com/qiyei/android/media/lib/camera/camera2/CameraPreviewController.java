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

    public CameraPreviewController(Context context,String tag) {
        super(context,tag,"CameraPreview");
    }

    public void setPreviewSurfaceTexture(SurfaceTexture texture){
        this.mPreviewSurfaceTexture = texture;
    }

    @Override
    public void prepare(CameraInfo cameraInfo) {
        if (mImageReader == null){
            mImageReader = buildImageReader(mTag, mSurfaceSize,ImageFormat.YUV_420_888,1);
        }
        if (mPreviewSurfaceTexture == null){
            Log.w(mTag,subTag + "prepare PreviewSurfaceTexture is null");
        }
    }

    @Override
    public void start(CameraInfo cameraInfo, CameraDevice cameraDevice, CameraCaptureSession captureSession) {
        CaptureRequest request = buildCaptureRequest(cameraInfo,cameraDevice);
        if (request == null){
            Log.e(mTag,subTag + "start error, CaptureRequest is null");
            return;
        }
        sendCaptureRequest(cameraInfo,cameraDevice,captureSession,request);
    }


    @Override
    public void onImageCallBack(Image image) {
        //Log.i(mTag, subTag + "##### ImageAvailableListener onImageAvailable,planes=" + image.getPlanes());
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
        mImageReader = buildImageReader(mTag, mSurfaceSize,ImageFormat.YUV_420_888,1);
    }


    @Override
    protected CaptureRequest buildCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice) {
        super.buildCaptureRequest(cameraInfo,cameraDevice);
        try {
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

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
    protected void sendCaptureRequest(CameraInfo cameraInfo,CameraDevice cameraDevice,CameraCaptureSession captureSession,CaptureRequest captureRequest) {
        super.sendCaptureRequest(cameraInfo,cameraDevice,captureSession,captureRequest);

        try {
            captureSession.setRepeatingRequest(captureRequest, new CameraCaptureSession.CaptureCallback() {
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
