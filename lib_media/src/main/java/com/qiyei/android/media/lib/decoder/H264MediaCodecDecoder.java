package com.qiyei.android.media.lib.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;


import com.qiyei.android.media.api.MediaConstant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;

public class H264MediaCodecDecoder extends MediaCodecDecoder{


    private boolean isFinish = false;

    public H264MediaCodecDecoder(Surface surface, int width, int height) {
        super(surface,width,height);

    }

    @Override
    public void start() {
        super.start();
        //开始解码
        mMediaCodec.start();
        Log.i(MediaConstant.H264_TAG,getTag() + "start");
        Executors.newFixedThreadPool(1).submit(new Runnable() {
            @Override
            public void run() {
                try {
                    while (isRunning) {
                        Log.i(MediaConstant.H264_TAG, getTag() + "run input=" + mInputPath);
                        File file = new File(mInputPath);
                        //判断文件是否存在
                        if (!file.exists()) {
                            Log.e(MediaConstant.H264_TAG, getTag() + "run error,inputPath =" + mInputPath + " is not exist !");
                            return;
                        }

                        FileInputStream fis = new FileInputStream(file);
                        //保存完整数据帧
                        byte[] frame = new byte[MediaConstant.FRAME_MAX_LEN];
                        //当前帧长度
                        int frameLen = 0;
                        //每次从文件读取的数据
                        byte[] readData = new byte[MediaConstant.BUFFER_READ_SIZE];
                        //开始时间
                        long startTime = System.currentTimeMillis();
                        //循环读取数据
                        while (!isFinish) {
                            if (fis.available() > 0) {
                                int readLen = fis.read(readData);
                                //当前长度小于最大值
                                if (frameLen + readLen < MediaConstant.FRAME_MAX_LEN) {
                                    //将readData拷贝到frame
                                    System.arraycopy(readData, 0, frame, frameLen, readLen);
                                    //修改frameLen
                                    frameLen += readLen;
                                    //寻找第一个帧头
                                    int headFirstIndex = findHead(frame, 0, frameLen);
                                    while (headFirstIndex >= 0 && isHead(frame, headFirstIndex)) {
                                        //寻找第二个帧头
                                        int headSecondIndex = findHead(frame, headFirstIndex + MediaConstant.FRAME_MIN_LEN, frameLen);
                                        //如果第二个帧头存在，则两个帧头之间的就是一帧完整的数据
                                        if (headSecondIndex > 0 && isHead(frame, headSecondIndex)) {
                                            Log.i(MediaConstant.H264_TAG,getTag() + "headSecondIndex:" + headSecondIndex);
                                            //视频解码
                                            onFrameDecoder(frame, headFirstIndex, headSecondIndex - headFirstIndex);
                                            //截取headSecondIndex之后到frame的有效数据,并放到frame最前面
                                            byte[] temp = Arrays.copyOfRange(frame, headSecondIndex, frameLen);
                                            System.arraycopy(temp, 0, frame, 0, temp.length);
                                            //修改frameLen的值
                                            frameLen = temp.length;
                                            //线程休眠
                                            sleepThread(startTime, System.currentTimeMillis());
                                            //重置开始时间
                                            startTime = System.currentTimeMillis();
                                            //继续寻找数据帧
                                            headFirstIndex = findHead(frame, 0, frameLen);
                                        } else {
                                            //找不到第二个帧头
                                            headFirstIndex = -1;
                                        }
                                    }
                                } else {
                                    //如果长度超过最大值，frameLen置0
                                    frameLen = 0;
                                }
                            } else {
                                //文件读取结束
                                isFinish = true;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected String getTag() {
        return "h264 decoder sync ";
    }

    /**
     * 寻找指定buffer中h264头的开始位置
     *
     * @param data   数据
     * @param offset 偏移量
     * @param max    需要检测的最大值
     * @return h264头的开始位置 ,-1表示未发现
     */
    private int findHead(byte[] data, int offset, int max) {
        int i;
        for (i = offset; i <= max; i++) {
            //发现帧头
            if (isHead(data, i))
                break;
        }
        //检测到最大值，未发现帧头
        if (i == max) {
            i = -1;
        }
        return i;
    }

    /**
     * 判断是否是I帧/P帧头:
     * 00 00 00 01 65    (I帧)
     * 00 00 00 01 61 / 41   (P帧)
     *
     * @param data
     * @param offset
     * @return 是否是帧头
     */
    private boolean isHead(byte[] data, int offset) {
        boolean result = false;
        // 00 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x00 && data[3] == 0x01 && isVideoFrameHeadType(data[offset + 4])) {
            result = true;
        }
        // 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x01 && isVideoFrameHeadType(data[offset + 3])) {
            result = true;
        }
        return result;
    }

    /**
     * I帧或者P帧
     */
    private boolean isVideoFrameHeadType(byte head) {
        return head == (byte) 0x65 || head == (byte) 0x61 || head == (byte) 0x41;
    }

    /**
     * 修眠
     * @param startTime
     * @param endTime
     */
    private void sleepThread(long startTime, long endTime) {
        //根据读文件和解码耗时，计算需要休眠的时间
        long time = MediaConstant.PRE_FRAME_TIME - (endTime - startTime);
        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
