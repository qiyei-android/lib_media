package com.qiyei.android.media.lib.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.qiyei.android.media.api.MediaConstant;
import com.qiyei.android.media.api.MediaMuxerListener;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomMediaMuxer {

    /**
     * 创建音频的 MediaExtractor
     */
    CustomMediaExtractor mAudioExtractor;
    /**
     * 创建视频的 MediaExtractor
     */
    CustomMediaExtractor mVideoExtractor;
    /**
     *
     */
    MediaMuxer mMediaMuxer;

    private int mAudioId;
    private int mVideoId;
    private MediaFormat mAudioFormat;
    private MediaFormat mVideoFormat;
    private MediaMuxerListener mMuxerListener;

    public CustomMediaMuxer(String inputPath,String outputPath) {
        mAudioExtractor = new CustomMediaExtractor(inputPath);
        mVideoExtractor = new CustomMediaExtractor(inputPath);

        mAudioFormat = mAudioExtractor.getAudioFormat();
        mVideoFormat = mVideoExtractor.getVideoFormat();

        try {
            mMediaMuxer = new MediaMuxer(outputPath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setMuxerListener(MediaMuxerListener muxerListener) {
        mMuxerListener = muxerListener;
    }

    public void start(){
        ExecutorService service = Executors.newFixedThreadPool(1);
        service.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mMuxerListener != null){
                        mMuxerListener.onStart();
                    }

                    mAudioId = mMediaMuxer.addTrack(mAudioFormat);
                    mVideoId = mMediaMuxer.addTrack(mVideoFormat);

                    mMediaMuxer.start();

                    ByteBuffer buffer = ByteBuffer.allocate(MediaConstant.DEFAULT_BUFFER_SIZE_IN_BYTES);
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                    int size = 0;
                    while ((size = mVideoExtractor.readBuffer(buffer,true)) > 0){
                        info.offset = 0;
                        info.size = size;
                        info.presentationTimeUs = mVideoExtractor.getCurSampleTime();
                        info.flags = mVideoExtractor.getCurSampleFlags();
                        mMediaMuxer.writeSampleData(mVideoId,buffer,info);
                    }

                    while ((size = mAudioExtractor.readBuffer(buffer,false)) > 0){
                        info.offset = 0;
                        info.size = size;
                        info.presentationTimeUs = mAudioExtractor.getCurSampleTime();
                        info.flags = mAudioExtractor.getCurSampleFlags();
                        mMediaMuxer.writeSampleData(mAudioId,buffer,info);
                    }

                    mAudioExtractor.release();
                    mVideoExtractor.release();
                    mMediaMuxer.stop();
                    mMediaMuxer.release();
                    if (mMuxerListener != null){
                        mMuxerListener.onStop();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
