package com.qiyei.android.media.lib.decoder;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.qiyei.android.media.api.MediaConstant;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CustomMediaExtractor {

    private MediaExtractor mMediaExtractor;

    private MediaFormat mVideoFormat;
    private int mVideoTrackId;

    private MediaFormat mAudioFormat;
    private int mAudioTrackId;

    private long mCurSampleTime;
    private int mCurSampleFlags;


    public CustomMediaExtractor(String path) {
        try {
            mMediaExtractor = new MediaExtractor();
            //设置数据源
            mMediaExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //获取所有的轨道
        int count = mMediaExtractor.getTrackCount();
        for (int i = 0;i < count ;i++){
            MediaFormat format = mMediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            //拿到视频轨道
            if (mime.startsWith(MediaConstant.MIME_VIDEO_PREFIX)) {
                mVideoTrackId = i;
                mVideoFormat = format;
                Log.i(MediaConstant.H264_TAG,getTag() + "init mVideoTrackId=" + mVideoTrackId + " mVideoFormat=" + mVideoFormat);

            } else if (mime.startsWith(MediaConstant.MIME_AUDIO_PREFIX)) {
                //拿到音频轨道
                mAudioTrackId = i;
                mAudioFormat = format;
                Log.i(MediaConstant.H264_TAG,getTag() + "init mAudioTrackId=" + mAudioTrackId + " mAudioFormat=" + mAudioFormat);

            }
        }

    }

    public void selectTrack(int trackId){
        mMediaExtractor.selectTrack(trackId);
    }


    public int readBuffer(ByteBuffer buffer,boolean video) {
        if (buffer == null){
            return -1;
        }
        buffer.clear();


        selectTrack(video ? mVideoTrackId : mAudioTrackId);
        //读取当前帧数据
        int bufferCount = mMediaExtractor.readSampleData(buffer,0);
        if (bufferCount < 0){
            return -1;
        }

        //记录当前时间戳及标志位
        mCurSampleTime = mMediaExtractor.getSampleTime();
        mCurSampleFlags = mMediaExtractor.getSampleFlags();
        Log.i(MediaConstant.H264_TAG,getTag() + "readBuffer mCurSampleTime=" + mCurSampleTime + " mCurSampleFlags=" + mCurSampleFlags);

        mMediaExtractor.advance();
        return bufferCount;
    }

    /**
     * 获取音频 MediaFormat
     * @return
     */
    public MediaFormat getAudioFormat() {
        return mAudioFormat;
    }

    /**
     * 获取视频 MediaFormat
     * @return
     */
    public MediaFormat getVideoFormat() {
        return mVideoFormat;
    }

    /**
     * 获取当前帧的标志位
     * @return
     */
    public int getCurSampleFlags() {
        return mCurSampleFlags;
    }

    /**
     * 获取当前帧的时间戳
     * @return
     */
    public long getCurSampleTime() {
        return mCurSampleTime;
    }

    /**
     * 释放资源
     */
    public void release() {
        mMediaExtractor.release();
    }

    protected String getTag(){
        return "CustomMediaExtractor";
    }
}
