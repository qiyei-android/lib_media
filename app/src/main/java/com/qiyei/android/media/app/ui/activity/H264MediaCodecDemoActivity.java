package com.qiyei.android.media.app.ui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;

import com.qiyei.android.media.api.ICamera2Api;
import com.qiyei.android.media.api.IEncoder;
import com.qiyei.android.media.api.MediaUtils;
import com.qiyei.android.media.app.R;
import com.qiyei.android.media.lib.camera.camera2.Camera2Impl;
import com.qiyei.android.media.lib.codec.H264MediaCodecEncoder;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class H264MediaCodecDemoActivity extends AppCompatActivity {

    private TextureView mTextureView;

    private boolean isRecord;

    private ICamera2Api mCamera2Api;

    private IEncoder mCodecEncoder;

    private byte[] y;
    private byte[] u;
    private byte[] v;

    private byte[] nv21;//width  height
    byte[] nv21_rotated;
    byte[] nv12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h264_media_codec_demo);

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
                        mCamera2Api.startPreview();
                    }

                    @Override
                    public void onImageAvailable(Image image) {
                        if (isRecord){
                            startMediaCodec(image);
                        } else {
                            stopMediaCodec();
                        }
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

        findViewById(R.id.capture_video_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRecord = !isRecord;
            }
        });
    }

    private void startMediaCodec(Image image){
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());

        if (mCodecEncoder == null){
            mCodecEncoder = new H264MediaCodecEncoder(image.getWidth(),image.getHeight());
            mCodecEncoder.setOutputPath(MediaUtils.getMediaStorePath() + File.separator + "mediacodec_" + dateFormat.format(System.currentTimeMillis()) + ".mp4");
            mCodecEncoder.start();
        }

        Image.Plane[] planes =  image.getPlanes();
        // 重复使用同一批byte数组，减少gc频率
        if (y == null) {
            y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
            u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
            v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
        }
        if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
            planes[0].getBuffer().get(y);
            planes[1].getBuffer().get(u);
            planes[2].getBuffer().get(v);
        }

        if (nv21 == null) {
            nv21 = new byte[planes[0].getRowStride() * image.getHeight() * 3 / 2];
            nv21_rotated = new byte[planes[0].getRowStride() * image.getHeight() * 3 / 2];
        }

        MediaUtils.yuvToNv21(y, u, v, nv21, planes[0].getRowStride(), image.getHeight());
        MediaUtils.nv21_rotate_to_90(nv21, nv21_rotated, planes[0].getRowStride(), image.getHeight());

        //获取到的数据拼接成nv21，需要转化成nv12，因为MediaCodec不支持nv21
        byte[] data = MediaUtils.nv21toNV12(nv21_rotated, nv12);

        Log.i("HHH","mCodecEncoder = " + mCodecEncoder);
        mCodecEncoder.enqueueData(data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCodecEncoder != null){
            mCodecEncoder.stop();
        }
    }

    private void stopMediaCodec(){
        if (mCodecEncoder != null){
            mCodecEncoder.stop();
            mCodecEncoder = null;
        }
    }
}