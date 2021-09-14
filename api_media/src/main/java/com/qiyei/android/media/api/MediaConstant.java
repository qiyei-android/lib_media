package com.qiyei.android.media.api;

import android.media.AudioFormat;
import android.media.MediaFormat;

public interface MediaConstant {

    String MIME_TYPE_VIDEO_AVC = "video/avc";
    String MIMETYPE_AUDIO_AAC = "audio/mp4a-latm";


    int H264_ENCODER = 1;

    int AAC_ENCODER = 2;

    int VALUE_FRAME_RATE = 60;

    int BUFFER_SIZE = 10;

    int TIME_OUT = 10000;

    /**
     * 默认采样频率
     */
    int DEFAULT_SAMPLE_RATE_IN_HZ = 44_100;

    int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    int DEFAULT_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    int DEFAULT_BUFFER_SIZE_IN_BYTES = 4096;
}
