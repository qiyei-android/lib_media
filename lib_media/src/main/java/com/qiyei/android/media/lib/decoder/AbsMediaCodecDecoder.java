package com.qiyei.android.media.lib.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import com.qiyei.android.media.api.DecoderCallBack;
import com.qiyei.android.media.api.IDecoder;
import com.qiyei.android.media.api.MediaConstant;

import java.io.IOException;

public abstract class AbsMediaCodecDecoder implements IDecoder {

    /**
     * 解码后显示的surface及其宽高
     */
    protected Surface mSurface;
    /**
     * 解码宽高
     */
    protected int mWidth, mHeight;
    /**
     * 解码器
     */
    protected MediaCodec mMediaCodec;
    /**
     * 解码回调
     */
    protected DecoderCallBack mCallBack;
    /**
     * 输入
     */
    protected String mInputPath;

    protected volatile boolean isRunning = false;

    protected boolean isFirst = true;

    public AbsMediaCodecDecoder(Surface surface, int width, int height) {
        mSurface = surface;
        mWidth = width;
        mHeight = height;
        try {
            //根据需要解码的类型创建解码器
            mMediaCodec = MediaCodec.createDecoderByType(MediaConstant.MIME_TYPE_VIDEO_AVC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //初始化MediaFormat
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaConstant.MIME_TYPE_VIDEO_AVC, width, height);
        //FPS 帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, MediaConstant.VALUE_FRAME_RATE);

        //配置MediaFormat以及需要显示的surface
        mMediaCodec.configure(mediaFormat, mSurface, null, 0);
        isFirst = false;
    }

    abstract protected String getTag();


    @Override
    public void setCallBack(DecoderCallBack callBack) {
        mCallBack = callBack;
    }

    @Override
    public void setInputPath(String path) {
        mInputPath = path;
    }

}
