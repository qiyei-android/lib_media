package com.qiyei.android.media.lib.decoder;

import android.media.MediaCodec;
import android.view.Surface;

import com.qiyei.android.media.api.MediaConstant;

import java.nio.ByteBuffer;

public class H264VideoMediaCodecDecoder extends MediaCodecDecoder {

    /**
     * 解码后显示的surface及其宽高
     */
    protected Surface mSurface;
    /**
     * 解码宽高
     */
    protected int mWidth, mHeight;

    public H264VideoMediaCodecDecoder(Surface surface, int width, int height,String path) {
        super(path);
        mSurface = surface;
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    protected String getTag() {
        return "Video H264 Decoder ";
    }

    @Override
    protected void configure(MediaCodec mediaCodec) {
        mediaCodec.configure(mMediaFormat, mSurface, null, 0);
    }

    @Override
    protected int getDecoderType() {
        return MediaConstant.VIDEO_H264_DECODER;
    }

    @Override
    protected void handlerFrameOutput(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {

    }
}
