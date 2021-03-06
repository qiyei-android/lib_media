package com.qiyei.android.media.lib.encoder;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.util.Log;


import com.qiyei.android.media.api.EncoderCallBack;
import com.qiyei.android.media.api.IEncoder;
import com.qiyei.android.media.api.MediaConstant;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Mp4MediaEncoderRecord implements IEncoder, EncoderCallBack {

    private final Object mLock;

    private AudioRecordLoader mAACMediaCodecEncoder;

    private H264MediaCodecEncoder mH264MediaCodecEncoder;

    private MediaMuxer mMediaMuxer;

    private volatile boolean mHasStartMuxer;
    private boolean mIsRecoding;
    private boolean mHasStopVideo;
    private boolean mHasStopAudio;
    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;

    private EncoderCallBack mCallBack;


    public Mp4MediaEncoderRecord(int width, int height, int sampleRateInHz, int channelConfig, String outputPath) {
        mH264MediaCodecEncoder = new H264MediaCodecEncoder(width, height);
        mAACMediaCodecEncoder = new AudioRecordLoader(MediaRecorder.AudioSource.MIC, sampleRateInHz,
                channelConfig, MediaConstant.DEFAULT_ENCODING,
                AudioRecord.getMinBufferSize(MediaConstant.DEFAULT_SAMPLE_RATE_IN_HZ, MediaConstant.DEFAULT_CHANNEL_CONFIG, MediaConstant.DEFAULT_ENCODING), null);
        mH264MediaCodecEncoder.setCallBack(this);
        mAACMediaCodecEncoder.setCallback(this);
        try {
            mMediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mHasStartMuxer = false;
        mLock = new Object();
    }

    @Override
    public void outputMediaFormatChanged(int type, MediaFormat mediaFormat) {
        try {
            synchronized (mLock) {
                if (mHasStartMuxer) {
                    return;
                }
                if (type == MediaConstant.VIDEO_H264_ENCODER) {
                    mVideoTrackIndex = mMediaMuxer.addTrack(mediaFormat);
                }
                if (type == MediaConstant.AUDIO_AAC_ENCODER) {
                    mAudioTrackIndex = mMediaMuxer.addTrack(mediaFormat);
                }
                Log.i(MediaConstant.H264_TAG,getTag() + "outputMediaFormatChanged mVideoTrackIndex=" + mVideoTrackIndex + " mAudioTrackIndex=" + mAudioTrackIndex);
                if (mVideoTrackIndex == -1 || mAudioTrackIndex == -1) {
                    mLock.wait();
                } else {
                    mMediaMuxer.start();
                    mHasStartMuxer = true;
                    mLock.notifyAll();
                }
            }

        } catch (Exception e) {
            Log.e("MFB", "error:" + e.getLocalizedMessage());
        }
    }

    @Override
    public void onEncodeOutput(int type, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        synchronized (mLock) {
            //write data
            if (type == MediaConstant.VIDEO_H264_ENCODER && mVideoTrackIndex != -1) {
                mMediaMuxer.writeSampleData(mVideoTrackIndex, byteBuffer, bufferInfo);
            }
            if (type == MediaConstant.AUDIO_AAC_ENCODER && mAudioTrackIndex != -1) {
                mMediaMuxer.writeSampleData(mAudioTrackIndex, byteBuffer, bufferInfo);
            }
        }
    }

    @Override
    public void onStop(int type) {
        synchronized (mLock) {
            if (type == MediaConstant.VIDEO_H264_ENCODER) {
                mHasStopVideo = true;
            }
            if (type == MediaConstant.AUDIO_AAC_ENCODER) {
                mHasStopAudio = true;
            }
            Log.i(MediaConstant.H264_TAG,getTag() + "onStop mHasStopAudio=" + mHasStopAudio + " mHasStopVideo=" + mHasStopVideo + " mHasStartMuxer=" + mHasStartMuxer);
            if (mHasStopAudio && mHasStopVideo && mHasStartMuxer) {
                mHasStartMuxer = false;
                mMediaMuxer.stop();
            }
        }
    }

    @Override
    public void setCallBack(EncoderCallBack callBack) {
        mCallBack = callBack;
    }

    @Override
    public void setOutputPath(String outputPath) {

    }

    @Override
    public void enqueueData(byte[] buffer) {
        mH264MediaCodecEncoder.enqueueData(buffer);
    }

    @Override
    public void start() {
        mIsRecoding = true;
        mAACMediaCodecEncoder.start();
        mH264MediaCodecEncoder.start();
    }

    @Override
    public void stop() {
        mAACMediaCodecEncoder.stop();
        mH264MediaCodecEncoder.stop();
        mIsRecoding = false;
    }

    private String getTag(){
        return "MP4 Codec ";
    }
}
