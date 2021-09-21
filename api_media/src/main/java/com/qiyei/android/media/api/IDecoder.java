package com.qiyei.android.media.api;



public interface IDecoder {

    void setCallBack(DecoderCallBack callBack);

    void start();

    void stop();
}
