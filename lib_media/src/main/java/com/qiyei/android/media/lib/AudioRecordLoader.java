package com.qiyei.android.media.lib;



import android.media.AudioRecord;


import com.qiyei.android.media.api.MediaConstant;
import com.qiyei.android.media.lib.codec.AACMediaCodecEncoder;
import com.qiyei.android.media.lib.codec.Mp4MediaCodecRecord;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author feibiao.ma
 * @project BlogDemo
 * @package_name com.poney.blogdemo.base.audio
 * @date 2021/1/8
 */
public class AudioRecordLoader {
    private boolean mIsRecording;
    private AudioRecord mAudioRecord;
    private AACMediaCodecEncoder mAACMediaCodecEncoder;


    public AudioRecordLoader(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, String outPath) {
        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        mAACMediaCodecEncoder = new AACMediaCodecEncoder(sampleRateInHz, channelConfig);
        mAACMediaCodecEncoder.setOutputPath(outPath);
    }


    public void start() {
        mAudioRecord.startRecording();
        mAACMediaCodecEncoder.start();
        mIsRecording = true;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[MediaConstant.DEFAULT_BUFFER_SIZE_IN_BYTES];
                while (mIsRecording) {
                    int len = mAudioRecord.read(buffer, 0, MediaConstant.DEFAULT_BUFFER_SIZE_IN_BYTES);
                    if (len > 0) {
                        byte[] data = new byte[len];
                        System.arraycopy(buffer, 0, data, 0, len);
                        mAACMediaCodecEncoder.enqueueData(data);
                    }
                }
            }
        });

    }

    public void stop() {
        mIsRecording = false;
        mAACMediaCodecEncoder.stop();
        mAudioRecord.stop();
    }

    public void release() {
        mAudioRecord.release();
        mAACMediaCodecEncoder = null;
        mAudioRecord = null;
    }

    public void setCallback(Mp4MediaCodecRecord mp4MediaCodecRecord) {
        mAACMediaCodecEncoder.setCallBack(mp4MediaCodecRecord);
    }
}
