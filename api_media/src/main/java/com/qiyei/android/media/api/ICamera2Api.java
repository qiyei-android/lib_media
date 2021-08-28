package com.qiyei.android.media.api;

import android.graphics.SurfaceTexture;
import android.util.Size;

public interface ICamera2Api {

    boolean open();

    void close();

    void setPreviewSurfaceTexture(SurfaceTexture texture);

    void setPreviewSize(Size size);

    void setImageSize(Size size);

    void createSession();

    //void setSessionListener(SurfaceTexture texture);

    void startPreview();

    void stopPreview();



}
