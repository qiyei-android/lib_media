package com.qiyei.android.media.lib.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.qiyei.android.media.api.CodecCallBack;
import com.qiyei.android.media.api.IMediaCodecEncoder;
import com.qiyei.android.media.api.MediaConstant;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class AbsMediaCodecEncoder implements IMediaCodecEncoder {

    protected volatile boolean isRunning = false;

    /**
     * 数据队列
     */
    protected ArrayBlockingQueue<byte[]> mYUV420Queue;

    /**
     * 编解码器
     */
    protected MediaCodec mMediaCodec;

    /**
     * 合成器
     */
    protected MediaMuxer mMediaMuxer;

    /**
     * 轨道index
     */
    protected int mTrackIndex;

    /**
     * 混合开始
     */
    protected boolean isMuxerStart;

    /**
     * 回调
     */
    protected CodecCallBack mCallBack;

    protected long prevOutputPTSUs;

    public AbsMediaCodecEncoder(int width,int height) {
        mYUV420Queue = new ArrayBlockingQueue<byte[]>(MediaConstant.BUFFER_SIZE);

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaConstant.MIME_TYPE_VIDEO_AVC,width,height);
        //YUV420
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        //码率
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,width * height * 5);
        //FPS 帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,MediaConstant.VALUE_FRAME_RATE);
        //I帧间隔
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaConstant.MIME_TYPE_VIDEO_AVC);
            mMediaCodec.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCallBack(CodecCallBack callBack) {
        mCallBack = callBack;
    }

    @Override
    public void setOutputPath(String outputPath) {
        try {
            mMediaMuxer = new MediaMuxer(outputPath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mTrackIndex = -1;
        isMuxerStart = false;
    }

    @Override
    public void enqueueData(byte[] buffer) {
        if (mYUV420Queue.size() >= MediaConstant.BUFFER_SIZE){
            mYUV420Queue.poll();
        }
        mYUV420Queue.add(buffer);
    }

    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        return Math.max(prevOutputPTSUs,result);
    }
}
