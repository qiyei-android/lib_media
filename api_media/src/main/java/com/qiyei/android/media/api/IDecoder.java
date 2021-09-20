package com.qiyei.android.media.api;



public interface IDecoder {

    void setCallBack(DecoderCallBack callBack);

    void setInputPath(String path);

    void onFrameDecoder(byte[] buf, int offset, int length);

    void start();

    void stop();
}
