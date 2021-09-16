package com.qiyei.android.media.lib;

import java.nio.ByteBuffer;

public class YUVUtils {


    static {
        System.loadLibrary("yuv");
    }


    public static native byte[] yuvToBuffer(ByteBuffer y,ByteBuffer u,ByteBuffer v,int yPixelStride,int yRowStride,
                            int uPixelStride,int uRowStride,int vPixelStride,int vRowStride,int width,int height);

}
