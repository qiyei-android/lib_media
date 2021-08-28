package com.qiyei.android.media.app.ui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Size;
import android.view.TextureView;

import com.qiyei.android.media.api.ICamera2Api;
import com.qiyei.android.media.app.R;
import com.qiyei.android.media.lib.camera.camera2.Camera2Impl;

public class Camera2DemoActivity extends AppCompatActivity {

    ICamera2Api mCamera2Api;
    private TextureView mTextureView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2_demo);
        mCamera2Api = new Camera2Impl(this);

        mTextureView = findViewById(R.id.camera_preview);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                mCamera2Api.setPreviewSurfaceTexture(surface);

                Size size = new Size(1080,1920);
                mCamera2Api.setPreviewSize(size);

                mCamera2Api.setImageSize(size);

                mCamera2Api.createSession();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });

        mCamera2Api.open();

    }

    @Override
    protected void onStart() {
        super.onStart();
        //mCamera2Api.createSession();

        mCamera2Api.startPreview();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCamera2Api.stopPreview();
    }
}