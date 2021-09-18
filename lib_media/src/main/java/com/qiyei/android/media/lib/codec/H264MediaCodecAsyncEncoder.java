package com.qiyei.android.media.lib.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import androidx.annotation.NonNull;

import com.qiyei.android.media.api.CodecCallBack;
import com.qiyei.android.media.api.MediaConstant;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class H264MediaCodecAsyncEncoder extends AbsMediaCodecEncoder{

    public H264MediaCodecAsyncEncoder(int width,int height) {
        super(width,height);
        try {
            mMediaCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    ByteBuffer inputBuffer = codec.getInputBuffer(index);
                    inputBuffer.clear();
                    byte[] input = null;
                    int length = 0;
                    if (isRunning){
                        if (!mYUV420Queue.isEmpty()){
                            input = mYUV420Queue.poll();
                        }
                        Log.i(MediaConstant.H264_TAG,getTag() + "run onInputBufferAvailable,index=" + index + " input=" + input);
                        if (input != null){
                            inputBuffer.put(input);
                            length = input.length;
                        }
                    }
                    codec.queueInputBuffer(index,0,length,getPTSUs(),0);
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                    Log.i(MediaConstant.H264_TAG,getTag() + "run onOutputBufferAvailable,index=" + index);
                    ByteBuffer outputBuffer = codec.getOutputBuffer(index);
                    if (info.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG){
                        info.size = 0;
                    }

                    if (info.size > 0){
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        outputBuffer.position(info.offset);
                        outputBuffer.limit(info.offset + info.size);
                        // write encoded data to muxer(need to adjust presentationTimeUs.
                        info.presentationTimeUs = getPTSUs();

                        if (mCallBack != null){
                            mCallBack.onEncodeOutput(MediaConstant.H264_ENCODER,outputBuffer,info);
                        }
                        prevOutputPTSUs = info.presentationTimeUs;
                        if (mMediaMuxer != null){
                            if (!isMuxerStart){
                                throw new RuntimeException("muxer hasn't started");
                            }
                            mMediaMuxer.writeSampleData(mTrackIndex,outputBuffer,info);
                        }
                    }
                    codec.releaseOutputBuffer(index,false);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    Log.i(MediaConstant.H264_TAG,getTag() + "run onError:" + e.getMessage());

                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    Log.i(MediaConstant.H264_TAG,getTag() + "run onOutputFormatChanged,format=" + format);
                    if (mCallBack != null){
                        mCallBack.outputMediaFormatChanged(MediaConstant.H264_ENCODER,format);
                    }

                    if (mMediaMuxer != null){
                        if (isMuxerStart){
                            throw new RuntimeException("format changed twice");
                        }

                        //now that we have the Magic Goodies, start the muxer
                        mTrackIndex = mMediaMuxer.addTrack(format);
                        mMediaMuxer.start();
                        isMuxerStart = true;
                    }
                }
            });

            mMediaCodec.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(){
        isRunning = true;
        Log.i(MediaConstant.H264_TAG,getTag() + "start");
    }

    @Override
    public void stop(){
        Log.i(MediaConstant.H264_TAG,getTag() + "stop");
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

    @Override
    protected String getTag() {
        return "h264 mediacodec async ";
    }
}
