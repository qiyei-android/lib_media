package com.qiyei.android.media.lib.codec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;

import com.qiyei.android.media.api.CodecCallBack;
import com.qiyei.android.media.api.MediaConstant;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;

public class AACMediaCodecEncoder extends AbsEncoder{

    private volatile boolean isRunning = false;

    /**
     *
     */
    private BufferedOutputStream mBufferedOutputStream;

    private MediaCodec mMediaCodec;

    private ArrayBlockingQueue<byte[]> mPcmQueue = new ArrayBlockingQueue<>(MediaConstant.BUFFER_SIZE);
    /**
     *
     */
    private int mSampleRateInHz;

    /**
     * 回调
     */
    protected CodecCallBack mCallBack;


    public AACMediaCodecEncoder(int sampleRateInHz, int channelConfig) {
        mSampleRateInHz = sampleRateInHz;
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaConstant.MIME_TYPE_AUDIO_AAC,sampleRateInHz, channelConfig == AudioFormat.CHANNEL_OUT_MONO ? 1 : 2);

        //声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,MediaConstant.DEFAULT_AUDIO_BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AudioRecord.getMinBufferSize(MediaConstant.DEFAULT_BUFFER_SIZE_IN_BYTES, MediaConstant.DEFAULT_CHANNEL_CONFIG, MediaConstant.DEFAULT_ENCODING));
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelConfig == AudioFormat.CHANNEL_OUT_MONO ? 1 : 2);

        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaConstant.MIME_TYPE_AUDIO_AAC);
            mMediaCodec.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCallBack(CodecCallBack callBack) {
        mCallBack = callBack;
    }

    @Override
    public void setOutputPath(String outputPath) {
        if (TextUtils.isEmpty(outputPath)){
            return;
        }

        File file = new File(outputPath);
        if (file.exists()){
            file.delete();
        }

        try {
            file.createNewFile();
            mBufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file),MediaConstant.DEFAULT_BUFFER_SIZE_IN_BYTES);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enqueueData(byte[] buffer) {
        if (mPcmQueue.size() >= MediaConstant.BUFFER_SIZE) {
            mPcmQueue.poll();
        }
        mPcmQueue.add(buffer);
        Log.d("HHH","add mPcmQueue.size=" + mPcmQueue.size());
    }

    @Override
    public void start() {
        isRunning = true;

        Executors.newFixedThreadPool(1).submit(new Runnable() {
            @Override
            public void run() {
                byte[] aacChunk;
                while (isRunning){
                    byte[] input = null;
                    if (mPcmQueue.size() > 0){
                        input = mPcmQueue.poll();
                    }
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
                                    mCallBack.outputMediaFormatChanged(MediaConstant.AAC_ENCODER,newFormat);
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
                                        mCallBack.onEncodeOutput(MediaConstant.AAC_ENCODER,outputBuffer,bufferInfo);
                                    }

                                    prevOutputPTSUs = bufferInfo.presentationTimeUs;
                                    if (mBufferedOutputStream != null) {
                                        //写入本地AAC文件
                                        aacChunk = new byte[bufferInfo.size + MediaConstant.ADTS_HEAD_LENGTH];
                                        addADTStoPacket(mSampleRateInHz, aacChunk, aacChunk.length);
                                        outputBuffer.get(aacChunk, MediaConstant.ADTS_HEAD_LENGTH, bufferInfo.size);
                                        try {
                                            mBufferedOutputStream.write(aacChunk);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
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

    @Override
    public void stop() {
        if (mCallBack != null){
            mCallBack.onStop(MediaConstant.AAC_ENCODER);
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

    private void addADTStoPacket(int sampleRateInHz, byte[] packet, int packetLen) {
        int profile = 2;
        int chanCfg = 2;
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (sampleRateInHz << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
