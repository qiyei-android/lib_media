package com.qiyei.android.media.app.ui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.media.Image;
import android.os.Bundle;
import android.util.Size;
import android.view.TextureView;
import android.view.View;

import com.qiyei.android.media.api.ICamera2Api;
import com.qiyei.android.media.app.R;
import com.qiyei.android.media.lib.camera.camera2.Camera2Impl;

public class Camera2DemoActivity extends AppCompatActivity {

    ICamera2Api mCamera2Api;
    private TextureView mTextureView;

    private boolean isAvailable;
    private boolean isRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2_demo);
        mCamera2Api = new Camera2Impl(this);
        mCamera2Api.init(0);


        mTextureView = findViewById(R.id.camera_preview);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                mCamera2Api.open(new ICamera2Api.CameraListener() {

                    @Override
                    public void onDeviceAvailable() {
                        mCamera2Api.start();
                    }

                    @Override
                    public void onSessionAvailable() {
                        isAvailable = true;
                        mCamera2Api.startPreview();
                    }

                    @Override
                    public void onImageAvailable(Image image) {

                    }
                });

                mCamera2Api.setPreviewSurfaceTexture(surface);

                Size size = new Size(1080,1920);
                mCamera2Api.setPreviewSize(size);

                mCamera2Api.setImageSize(size);

                mCamera2Api.setRecordSize(size);
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

        findViewById(R.id.capture_image_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera2Api.takePhoto();
            }
        });

        findViewById(R.id.capture_video_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecord){
                    mCamera2Api.stopRecord();
                } else {
                    mCamera2Api.startRecord();
                }
                isRecord = !isRecord;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isAvailable){
            mCamera2Api.startPreview();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isAvailable = false;
        mCamera2Api.stop();
    }
}