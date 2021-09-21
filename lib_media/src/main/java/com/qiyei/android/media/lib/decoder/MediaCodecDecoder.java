package com.qiyei.android.media.lib.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.qiyei.android.media.api.DecoderCallBack;
import com.qiyei.android.media.api.IDecoder;
import com.qiyei.android.media.api.MediaConstant;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;

public abstract class MediaCodecDecoder implements IDecoder {

    /**
     * 解码类型
     */
    protected int mType;
    /**
     * 解码器
     */
    protected MediaCodec mMediaCodec;
    /**
     * 解码format
     */
    protected MediaFormat mMediaFormat;
    /**
     * 媒体提取器
     */
    CustomMediaExtractor mMediaExtractor;

    /**
     * 解码回调
     */
    protected DecoderCallBack mCallBack;
    /**
     * 输入
     */
    protected String mInputPath;

    /**
     * 是否结束
     */
    protected volatile boolean isFinish = false;

    public MediaCodecDecoder(String inputPath) {
        try {
            mInputPath = inputPath;
            //判断是音频还是视频
            mType = getDecoderType();
            mMediaExtractor = new CustomMediaExtractor(mInputPath);
            //拿到音频或视频的 MediaFormat
            mMediaFormat = (mType == MediaConstant.VIDEO_H264_DECODER ? mMediaExtractor.getVideoFormat() : mMediaExtractor.getAudioFormat());
            mMediaExtractor.selectTrack(mType == MediaConstant.VIDEO_H264_DECODER ? mMediaExtractor.getVideoTrackId() : mMediaExtractor.getAudioTrackId());
            //根据需要解码的类型创建解码器
            mMediaCodec = MediaCodec.createDecoderByType(mMediaFormat.getString(MediaFormat.KEY_MIME));
            //配置MediaCodec
            configure(mMediaCodec);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    abstract protected String getTag();


    @Override
    public void setCallBack(DecoderCallBack callBack) {
        mCallBack = callBack;
    }

    @Override
    public void start() {
        mMediaCodec.start();
        isFinish = false;
        Log.i(MediaConstant.H264_TAG,getTag() + "start");
        Executors.newFixedThreadPool(1).submit(new Runnable() {
            @Override
            public void run() {
                while (!isFinish) {
                    ByteBuffer buffer = ByteBuffer.allocate(MediaConstant.BUFFER_READ_SIZE);
                    int size = mMediaExtractor.readBuffer(buffer);
                    Log.i(MediaConstant.H264_TAG,getTag() + "run MediaExtractor.readBuffer=" + size );
                    onFrameDecoder(buffer.array(),0,size,mMediaExtractor.getCurSampleTime(),mMediaExtractor.getCurSampleFlags());
                }
            }
        });

    }




    @Override
    public void stop() {
        if (mMediaCodec != null){
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }

        if (mMediaExtractor != null){
            mMediaExtractor.release();
        }

    }

    protected abstract void configure(MediaCodec mediaCodec);

    protected abstract int getDecoderType();

    protected void onFrameDecoder(byte[] buf, int offset, int length,long presentationTimeUs, int flags) {
        //获取输入buffer index -1表示一直等待；0表示不等待；其他大于0的参数表示等待毫秒数
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(MediaConstant.TIME_OUT_US);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
            //清空buffer
            inputBuffer.clear();
            if (length >= 0){
                //put需要解码的数据
                inputBuffer.put(buf, offset, length);
                //解码
                mMediaCodec.queueInputBuffer(inputBufferIndex, offset, length, presentationTimeUs, flags);
                Log.i(MediaConstant.H264_TAG,getTag() + "run inputBufferIndex=" + inputBufferIndex + " length=" + length);
            } else {
                //结束
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                Log.i(MediaConstant.H264_TAG,getTag() + "run inputBufferIndex=" + inputBufferIndex + " end");
            }
        }

        // 获取输出buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, MediaConstant.TIME_OUT_US);
//        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
//            MediaFormat newFormat = mMediaCodec.getOutputFormat();
//            if (mCallBack != null){
//                mCallBack.outputMediaFormatChanged(MediaConstant.VIDEO_H264_ENCODER,newFormat);
//            }
//        }

        // 在所有解码后的帧都被渲染后，就可以停止播放了
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.i(MediaConstant.H264_TAG, getTag() + " finish 1,OutputBuffer BUFFER_FLAG_END_OF_STREAM");
            isFinish = true;
        }

        if (getDecoderType() == MediaConstant.VIDEO_H264_DECODER){
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
        } else {
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
        }

//        //循环解码，直到数据全部解码完成
//        while (outputBufferIndex >= 0) {
//            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
//            //解码配置
//            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG){
//                bufferInfo.size = 0;
//            }
//
//            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                Log.i(MediaConstant.H264_TAG, getTag() + " finish 2,OutputBuffer BUFFER_FLAG_END_OF_STREAM");
//                isFinish = true;
//            }
//
//            if (bufferInfo.size > 0){
//                // adjust the ByteBuffer values to match BufferInfo (not needed?)
//                outputBuffer.position(bufferInfo.offset);
//                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
//                // write encoded data to muxer(need to adjust presentationTimeUs.
//
//                if (mCallBack != null){
//                    mCallBack.onDecodeOutput(MediaConstant.VIDEO_H264_DECODER,outputBuffer,bufferInfo);
//                }
//            }
//
//            if (getDecoderType() == MediaConstant.VIDEO_H264_DECODER){
//                mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
//            } else {
//                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
//            }
//            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, MediaConstant.TIME_OUT_US);
//        }

    }
}
