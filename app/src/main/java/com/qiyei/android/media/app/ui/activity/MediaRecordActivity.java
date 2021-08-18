package com.qiyei.android.media.app.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.qiyei.android.media.app.R;
import com.qiyei.android.media.app.ui.view.CameraPreview;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Created by qiyei2015 on 2018/8/18.
 * @version: 1.0
 * @email: 1273482124@qq.com
 * @description:
 */
public class MediaRecordActivity extends Activity {

    private static final String TAG = "MediaRecordActivity";

    /**
     * 需要的动态权限
     */
    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * 权限码
     */
    private static final int PERMISSIONS_REQUEST_VIDEO = 1;

    private CameraPreview mCameraPreview;

    private Button mCaptureButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_record);
        mCaptureButton = findViewById(R.id.button);
        mCaptureButton.setText("录制视频");
        mCaptureButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (mCameraPreview.isRecording()){
                    mCameraPreview.stopRecording();
                    mCaptureButton.setText("录制视频");
                }else {
                    mCameraPreview.startRecording();
                    mCaptureButton.setText("停止");
                }
            }
        });
        //检查权限申请
        checkVideoPermission();
        initCameraPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraPreview = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraPreview == null){
            initCameraPreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_VIDEO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionsResult granted");
                    initCameraPreview();
                } else {
                    Log.i(TAG, "onRequestPermissionsResult denied");
                    Toast.makeText(this, "申请权限失败", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    private void initCameraPreview(){
        mCameraPreview = new CameraPreview(this);
        FrameLayout layout = findViewById(R.id.camera_preview);
        layout.addView(mCameraPreview);
    }

    private void checkVideoPermission(){
        for (String permission:VIDEO_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSIONS_REQUEST_VIDEO);
            }
        }
    }
}
