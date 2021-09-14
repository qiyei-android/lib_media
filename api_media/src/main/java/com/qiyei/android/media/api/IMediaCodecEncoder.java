package com.qiyei.android.media.api;



public interface IMediaCodecEncoder {

    void setCallBack(CodecCallBack callBack);

    void setOutputPath(String outputPath);

    void enqueueData(byte[] buffer);

    void start();

    void stop();
}
