package com.qiyei.android.media.api;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public interface EncoderCallBack {
    void outputMediaFormatChanged(int type, MediaFormat mediaFormat);

    void onEncodeOutput(int type, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);

    void onStop(int type);
}