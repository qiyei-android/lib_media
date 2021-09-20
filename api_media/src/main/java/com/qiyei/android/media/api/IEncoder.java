package com.qiyei.android.media.api;



public interface IEncoder {

    void setCallBack(EncoderCallBack callBack);

    void setOutputPath(String outputPath);

    void enqueueData(byte[] buffer);

    void start();

    void stop();
}
