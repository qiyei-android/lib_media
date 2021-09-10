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

public class H264MediaCodecAsyncEncoder {

    private volatile boolean isRunning = false;

    /**
     * 数据队列
     */
    private ArrayBlockingQueue<byte[]> mYUV420Queue;

    /**
     * 编解码器
     */
    private MediaCodec mMediaCodec;
    /**
     * 合成器
     */
    private MediaMuxer mMediaMuxer;

    /**
     * 轨道index
     */
    private int mTrackIndex;
    /**
     * 混合开始
     */
    private boolean isMuxerStart;

    private CodecCallBack mCallBack;

    private long prevOutputPTSUs;


    public H264MediaCodecAsyncEncoder(int width,int height) {
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
            mMediaCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    Log.i("MFB", "onInputBufferAvailable:" + index);

                    byte[] input = null;
                    if (isRunning){
                        if (!mYUV420Queue.isEmpty()){
                            input = mYUV420Queue.poll();
                        }

                        if (input != null){
                            ByteBuffer inputBuffer = codec.getInputBuffer(index);
                            inputBuffer.clear();
                            inputBuffer.put(input);
                            codec.queueInputBuffer(index,0,input.length,getPTSUs(),0);
                        }
                    }

                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                    Log.i("MFB", "onOutputBufferAvailable:" + index);
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
                    Log.i("MFB", "onError,CodecException:" + e.getMessage());

                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    Log.i("MFB", "onOutputFormatChanged,format=:" + format);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOutputPath(String outputPath){
        try {
            mMediaMuxer = new MediaMuxer(outputPath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mTrackIndex = -1;
        isMuxerStart = false;
    }

    public void enqueueData(byte[] buffer){
        if (mYUV420Queue.size() >= MediaConstant.BUFFER_SIZE){
            mYUV420Queue.poll();
        }
        mYUV420Queue.add(buffer);
    }

    public void start(){
        isRunning = true;
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

    private long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        return Math.max(prevOutputPTSUs,result);
    }
}
