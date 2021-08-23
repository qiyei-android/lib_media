package com.qiyei.android.media.app.ui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.qiyei.android.media.api.ICameraApi;
import com.qiyei.android.media.app.R;
import com.qiyei.android.media.lib.CameraProxy;

import java.lang.ref.WeakReference;

public class CameraDemoActivity extends AppCompatActivity {

    private static final String TAG = "CameraDemoActivity";

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


    private ICameraApi mCameraApi;
    private SurfaceView mSurfaceView;

    //预览数据回调
    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.i(TAG,"onPreviewFrame data.length=" + data.length);
        }
    };

    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            int degree = 0;
            Log.i(TAG,"surfaceCreated ");
            mCameraApi.setPreviewOrientation(Configuration.ORIENTATION_PORTRAIT,degree);
            mCameraApi.setPreviewResolution(1920,1080);
            mCameraApi.setSurfaceHolderRef(new WeakReference<>(holder));
            mCameraApi.setPreviewCallback(mPreviewCallback);
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            mCameraApi.close();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_demo);

        mCameraApi = CameraProxy.getInstance();
        mSurfaceView = findViewById(R.id.surfaceView);

        //检查权限申请
        checkVideoPermission();
        initCamera();

        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraApi.start();
            }
        });

        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraApi.stop();
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSurfaceView.getHolder().removeCallback(mCallback);
        mCameraApi.close();
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
                    initCamera();
                } else {
                    Log.i(TAG, "onRequestPermissionsResult denied");
                    Toast.makeText(this, "申请权限失败", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void initCamera(){
        if (mCameraApi.open()){
            mSurfaceView.getHolder().addCallback(mCallback);
        }
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