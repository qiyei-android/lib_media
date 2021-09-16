package com.qiyei.android.media.lib.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.qiyei.android.media.api.CodecCallBack;
import com.qiyei.android.media.api.MediaConstant;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;

public class H264MediaCodecEncoder extends AbsMediaCodecEncoder{

    public H264MediaCodecEncoder(int width,int height) {
        super(width,height);
        mMediaCodec.start();
    }

    public void start(){
        isRunning = true;

        Executors.newFixedThreadPool(1).submit(new Runnable() {
            @Override
            public void run() {
                while (isRunning){
                    Log.i("HHH","mYUV420Queue.size=" + mYUV420Queue.size());
                    byte[] input = mYUV420Queue.poll();
                    try {
                        if (input != null){
                            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(MediaConstant.TIME_OUT);
                            Log.i("HHH","inputBufferIndex=" + inputBufferIndex);
                            if (inputBufferIndex >= 0){
                                ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                                inputBuffer.clear();
                                inputBuffer.put(input);
                                mMediaCodec.queueInputBuffer(inputBufferIndex,0,input.length,getPTSUs(),0);
                            }

                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,MediaConstant.TIME_OUT);
                            Log.i("HHH","outputBufferIndex=" + outputBufferIndex);
                            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                                MediaFormat newFormat = mMediaCodec.getInputFormat();
                                if (mCallBack != null){
                                    mCallBack.outputMediaFormatChanged(MediaConstant.H264_ENCODER,newFormat);
                                }
                                if (mMediaMuxer != null){
                                    if (isMuxerStart){
                                        throw new RuntimeException("format changed twice");
                                    }
                                    // now that we have the Magic Goodies, start the muxer
                                    mTrackIndex = mMediaMuxer.addTrack(newFormat);
                                    mMediaMuxer.start();
                                    isMuxerStart = true;
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
                                        mCallBack.onEncodeOutput(MediaConstant.H264_ENCODER,outputBuffer,bufferInfo);
                                    }

                                    prevOutputPTSUs = bufferInfo.presentationTimeUs;
                                    if (mMediaMuxer != null){
                                        if (!isMuxerStart){
                                            throw new RuntimeException("muxer hasn't started");
                                        }
                                        mMediaMuxer.writeSampleData(mTrackIndex,outputBuffer,bufferInfo);
                                    }
                                }

                                mMediaCodec.releaseOutputBuffer(outputBufferIndex,false);
                                bufferInfo = new MediaCodec.BufferInfo();
                                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,MediaConstant.TIME_OUT);
                                Log.i("HHH","release after outputBufferIndex=" + outputBufferIndex);
                            }

                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void stop(){
        if (mCallBack != null){
            mCallBack.onStop(MediaConstant.H264_ENCODER);
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

}
