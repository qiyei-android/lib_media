package com.qiyei.android.media.app.ui.activity;

import android.graphics.SurfaceTexture;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.qiyei.android.media.api.ICamera2Api;
import com.qiyei.android.media.api.IEncoder;
import com.qiyei.android.media.api.MediaConstant;
import com.qiyei.android.media.api.MediaUtils;
import com.qiyei.android.media.app.R;
import com.qiyei.android.media.lib.camera.camera2.Camera2Impl;
import com.qiyei.android.media.lib.codec.H264MediaCodecAsyncEncoder;
import com.qiyei.android.media.lib.codec.Mp4MediaCodecRecord;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class H264MP4DemoActivity extends AppCompatActivity {

    private TextureView mTextureView;

    private boolean isRecord;

    private ICamera2Api mCamera2Api;

    private IEncoder mCodecEncoder;

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
            mCodecEncoder = new Mp4MediaCodecRecord(image.getWidth(),image.getHeight(), MediaConstant.DEFAULT_SAMPLE_RATE_IN_HZ,MediaConstant.DEFAULT_CHANNEL_CONFIG,MediaUtils.getMediaStorePath() + File.separator + "mp4demo_" + dateFormat.format(System.currentTimeMillis()) + ".mp4");
            //mCodecEncoder.setOutputPath();
            mCodecEncoder.start();
        }
        byte[] data = MediaUtils.nv21ToYUV420(MediaUtils.convertToNV21(image),image.getWidth(),image.getHeight());
//        final Image.Plane[] planes = image.getPlanes();
//        Image.Plane yPlane = planes[0];
//        Image.Plane uPlane = planes[1];
//        Image.Plane vPlane = planes[2];
//        byte[] data = YUVUtils.yuvToBuffer(yPlane.getBuffer(), uPlane.getBuffer(), vPlane.getBuffer(),
//                yPlane.getPixelStride(), yPlane.getRowStride(), uPlane.getPixelStride(), uPlane.getRowStride(),
//                vPlane.getPixelStride(), vPlane.getRowStride(), image.getWidth(), image.getHeight());

        mCodecEncoder.enqueueData(data);
        Log.d("MFB","startMediaCodec");
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