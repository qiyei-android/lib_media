package com.qiyei.android.media.lib.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.qiyei.android.media.api.MediaMuxerListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomMediaMuxer {
    /**
     * 创建视频的 MediaExtractor
     */
    CustomMediaExtractor mMediaExtractor;
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
        mMediaExtractor = new CustomMediaExtractor(inputPath);

        mAudioFormat = mMediaExtractor.getAudioFormat();
        mVideoFormat = mMediaExtractor.getVideoFormat();

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

                    //尽量大一些
                    ByteBuffer buffer = ByteBuffer.allocate(200 * 1024);
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                    int size = 0;
                    mMediaExtractor.selectTrack(mMediaExtractor.getVideoTrackId());
                    while ((size = mMediaExtractor.readBuffer(buffer)) > 0){
                        info.offset = 0;
                        info.size = size;
                        info.presentationTimeUs = mMediaExtractor.getCurSampleTime();
                        info.flags = mMediaExtractor.getCurSampleFlags();
                        mMediaMuxer.writeSampleData(mVideoId,buffer,info);
                    }

                    mMediaExtractor.selectTrack(mMediaExtractor.getAudioTrackId());
                    while ((size = mMediaExtractor.readBuffer(buffer)) > 0){
                        info.offset = 0;
                        info.size = size;
                        info.presentationTimeUs = mMediaExtractor.getCurSampleTime();
                        info.flags = mMediaExtractor.getCurSampleFlags();
                        mMediaMuxer.writeSampleData(mAudioId,buffer,info);
                    }

                    //mAudioExtractor.release();
                    mMediaExtractor.release();
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
