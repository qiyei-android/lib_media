package com.qiyei.android.media.lib.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.qiyei.android.media.api.MediaConstant;


import java.nio.ByteBuffer;


public class MediaCodecDecoder extends AbsMediaCodecDecoder {

    private int mCount = 0;

    public MediaCodecDecoder(Surface surface, int width, int height) {
        super(surface,width,height);

    }

    @Override
    public void onFrameDecoder(byte[] buf, int offset, int length) {
        //获取输入buffer index -1表示一直等待；0表示不等待；其他大于0的参数表示等待毫秒数
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(MediaConstant.TIME_OUT);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
            //清空buffer
            inputBuffer.clear();
            //put需要解码的数据
            inputBuffer.put(buf, offset, length);
            //解码
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, System.currentTimeMillis(), 0);
            mCount++;
        } else {
            return ;
        }

        // 获取输出buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, MediaConstant.TIME_OUT);
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
            MediaFormat newFormat = mMediaCodec.getOutputFormat();
            if (mCallBack != null){
                mCallBack.outputMediaFormatChanged(MediaConstant.H264_ENCODER,newFormat);
            }
        }
        //循环解码，直到数据全部解码完成
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
            //解码配置
            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG){
                bufferInfo.size = 0;
            }
            if (bufferInfo.size > 0){
                // adjust the ByteBuffer values to match BufferInfo (not needed?)
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                // write encoded data to muxer(need to adjust presentationTimeUs.

                if (mCallBack != null){
                    mCallBack.onDecodeOutput(MediaConstant.H264_DECODER,outputBuffer,bufferInfo);
                }
            }

            if (mSurface != null){
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            } else {
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, MediaConstant.TIME_OUT);
        }
    }


    @Override
    public void start() {
        isRunning = true;
        mCount = 0;
    }

    @Override
    public void stop() {
        Log.i(MediaConstant.H264_TAG,getTag() + "stop");
        if (mCallBack != null){
            mCallBack.onStop(MediaConstant.H264_ENCODER);
        }
        isRunning = false;
        try {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getTag() {
        return "mediacodec decoder sync ";
    }


}
