package com.qiyei.android.media.api;

import android.graphics.SurfaceTexture;
import android.util.Size;

public interface ICamera2Api {

    boolean open();

    void close();

    void setPreviewSurfaceTexture(SurfaceTexture texture);

    void setPreviewSize(Size size);

    void setImageSize(Size size);

    void setRecordSize(Size size);

    void createSession();

    void setSessionListener(CameraCaptureSessionListener listener);

    void startPreview();

    void stopPreview();

    void takePhoto();

    void startRecord();

    interface CameraCaptureSessionListener {
        void onAvailable();
    }
}
