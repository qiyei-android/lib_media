package com.qiyei.android.media.api;

import android.media.AudioFormat;
import android.media.MediaFormat;

public interface MediaConstant {

    String H264_TAG = "[H264]";

    String MIME_TYPE_VIDEO_AVC = MediaFormat.MIMETYPE_VIDEO_AVC;

    String MIME_TYPE_AUDIO_AAC = MediaFormat.MIMETYPE_AUDIO_AAC;


    int H264_ENCODER = 1;

    int AAC_ENCODER = 2;

    int H264_DECODER = 3;

    int AAC_DECODER = 4;

    int VALUE_FRAME_RATE = 30;

    int BUFFER_SIZE = 10;

    int TIME_OUT = 10000;

    /**
     * 默认采样频率
     */
    int DEFAULT_SAMPLE_RATE_IN_HZ = 44_100;

    int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    int DEFAULT_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    int DEFAULT_BUFFER_SIZE_IN_BYTES = 4096;

    int DEFAULT_AUDIO_BIT_RATE = 96000;

    int ADTS_HEAD_LENGTH = 7;

    String MIME_VIDEO_PREFIX = "video";

    String MIME_AUDIO_PREFIX = "audio";

    /**
     * 这个值用于找到第一个帧头后，继续寻找第二个帧头，如果解码失败可以尝试缩小这个值
     */
    int FRAME_MIN_LEN = 1024;

    /**
     * 一般H264帧大小不超过200k,如果解码失败可以尝试增大这个值
     */
    int FRAME_MAX_LEN = 300 * 1024;

    /**
     * 根据帧率获取的解码每帧需要休眠的时间,根据实际帧率进行操作
     */
    int PRE_FRAME_TIME = 1000 / VALUE_FRAME_RATE;

    int BUFFER_READ_SIZE = 10 * 1024;
}
