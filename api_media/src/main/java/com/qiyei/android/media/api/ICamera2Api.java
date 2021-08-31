package com.qiyei.android.media.api;

import android.graphics.SurfaceTexture;
import android.util.Size;

public interface ICamera2Api {

    /**
     * 初始化
     * @param type
     */
    void init(int type);

    /**
     * 打开相机
     * @return
     */
    boolean open(CameraListener listener);

    /**
     * 关闭相机
     */
    void close();

    /**
     * 设置预览
     * @param texture
     */
    void setPreviewSurfaceTexture(SurfaceTexture texture);

    /**
     * 设置预览Size
     * @param size
     */
    void setPreviewSize(Size size);

    /**
     * 设置ImageSize
     * @param size
     */
    void setImageSize(Size size);

    /**
     * 设置录制大小
     * @param size
     */
    void setRecordSize(Size size);

    /**
     * 开始操作
     */
    void start();

    /**
     * 停止操作
     */
    void stop();

    /**
     * 开始预览
     */
    void startPreview();

    /**
     * 停止预览
     */
    void stopPreview();

    /**
     * 拍照
     */
    void takePhoto();

    /**
     * 开始录制
     */
    void startRecord();

    /**
     * 停止录制
     */
    void stopRecord();


    interface CameraListener {
        /**
         * Session 可用
         */
        void onDeviceAvailable();
        /**
         * Session 可用
         */
        void onSessionAvailable();
    }

}
