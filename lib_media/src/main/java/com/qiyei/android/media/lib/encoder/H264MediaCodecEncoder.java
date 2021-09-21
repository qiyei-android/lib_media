package com.qiyei.android.media.lib.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.qiyei.android.media.api.MediaConstant;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;

public class H264MediaCodecEncoder extends AbsMediaCodecEncoder{

    public H264MediaCodecEncoder(int width,int height) {
        super(width,height);
        mMediaCodec.start();
    }

    public void start(){
        isRunning = true;
        Log.i(MediaConstant.H264_TAG,getTag() + "start");
        Executors.newFixedThreadPool(1).submit(new Runnable() {
            @Override
            public void run() {
                byte[] input = null;
                while (isRunning){
                    Log.i(MediaConstant.H264_TAG,getTag() + "run mYUV420Queue.size=" + mYUV420Queue.size());
                    if (mYUV420Queue.size() > 0) {
                        input = mYUV420Queue.poll();
                    }
                    try {
                        if (input != null){
                            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(MediaConstant.TIME_OUT_US);
                            Log.i(MediaConstant.H264_TAG,getTag() + "run inputBufferIndex=" + inputBufferIndex);
                            if (inputBufferIndex >= 0){
                                ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                                inputBuffer.clear();
                                inputBuffer.put(input);
                                mMediaCodec.queueInputBuffer(inputBufferIndex,0,input.length,getPTSUs(),0);
                            }

                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,MediaConstant.TIME_OUT_US);
                            Log.i(MediaConstant.H264_TAG,getTag() + "run outputBufferIndex=" + outputBufferIndex);
                            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                                if (mCallBack != null){
                                    mCallBack.outputMediaFormatChanged(MediaConstant.VIDEO_H264_ENCODER,newFormat);
                                }
                                if (mMediaMuxer != null){
                                    if (isMuxerStart){
                                        throw new RuntimeException("format changed twice");
                                    }
                                    // now that we have the Magic Goodies, start the muxer
                                    mTrackIndex = mMediaMuxer.addTrack(newFormat);
                                    mMediaMuxer.start();
                                    isMuxerStart = true;
                                    Log.i(MediaConstant.H264_TAG,getTag() + "run MediaMuxer.start mTrackIndex=" + mTrackIndex);
                                }
                            }

                            while (outputBufferIndex >= 0){
                                ByteBuffer outputBuffer = null;
                                outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                                //编码配置
                                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG){
                                    bufferInfo.size = 0;
                                }

                                if (bufferInfo.size > 0){
                                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                                    outputBuffer.position(bufferInfo.offset);
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                                    // write encoded data to muxer(need to adjust presentationTimeUs.

                                    bufferInfo.presentationTimeUs = getPTSUs();

                                    if (mCallBack != null){
                                        mCallBack.onEncodeOutput(MediaConstant.VIDEO_H264_ENCODER,outputBuffer,bufferInfo);
                                    }

                                    prevOutputPTSUs = bufferInfo.presentationTimeUs;
                                    if (mMediaMuxer != null){
                                        if (!isMuxerStart){
                                            throw new RuntimeException("muxer hasn't started");
                                        }
                                        Log.i(MediaConstant.H264_TAG,getTag() + "run writeSampleData mTrackIndex=" + mTrackIndex);
                                        mMediaMuxer.writeSampleData(mTrackIndex,outputBuffer,bufferInfo);
                                    }
                                }

                                mMediaCodec.releaseOutputBuffer(outputBufferIndex,false);
                                bufferInfo = new MediaCodec.BufferInfo();
                                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,MediaConstant.TIME_OUT_US);
                                Log.i(MediaConstant.H264_TAG,getTag() + "run release after outputBufferIndex=" + outputBufferIndex);
                            }

                        } else {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void stop(){
        Log.i(MediaConstant.H264_TAG,getTag() + "stop");
        if (mCallBack != null){
            mCallBack.onStop(MediaConstant.VIDEO_H264_ENCODER);
        }

        isRunning = false;
        try {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
            if (mMediaMuxer != null){
                mMediaMuxer.stop();
                mMediaMuxer.release();
                mMediaMuxer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getTag() {
        return "h264 mediacodec sync ";
    }
}
