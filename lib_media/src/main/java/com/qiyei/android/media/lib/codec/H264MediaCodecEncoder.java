package com.qiyei.android.media.lib.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.qiyei.android.media.api.CodecCallBack;
import com.qiyei.android.media.api.MediaConstant;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class H264MediaCodecEncoder {

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

    public H264MediaCodecEncoder(int width,int height) {
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
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setCallBack(CodecCallBack callBack) {
        mCallBack = callBack;
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

        byte[] input = null;
        while (isRunning){
            if (!mYUV420Queue.isEmpty()){
                input = mYUV420Queue.poll();
            }

            try {
                if (input != null){
                    int inputBufferIndex = mMediaCodec.dequeueInputBuffer(MediaConstant.TIME_OUT);
                    if (inputBufferIndex >= 0){
                        ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                        inputBuffer.clear();
                        inputBuffer.put(input);
                        mMediaCodec.queueInputBuffer(inputBufferIndex,0,input.length,getPTSUs(),0);
                    }

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                    int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,MediaConstant.TIME_OUT);
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
                    }
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
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
