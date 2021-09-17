package com.qiyei.android.media.api;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.os.Environment;

import java.io.File;
import java.nio.ByteBuffer;

public class MediaUtils {

    public static String getMediaStorePath(){
        String cameraDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator +  "Camera";
        return cameraDir;
    }

    /**
     * Image数据转换为NV21
     * @param image
     * @return
     */
    public static final byte[] convertToNV21(Image image) {

        Rect crop = image.getCropRect();

        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        //YUV
        Image.Plane[] planes = image.getPlanes();

        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        int channelOffset = 0;
        int outputStride = 1;

        for(int i = 0; i < planes.length; ++i) {
            switch(i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
            }

            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();

            int shift = (i == 0 ? 0 : 1);
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));


            for(int j = 0; j < h; j++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, w);
                    channelOffset += w;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);

                    for(int k = 0; k < w; k++) {
                        data[channelOffset] = rowData[k * pixelStride];
                        channelOffset += outputStride;
                    }
                }

                if (j < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }

        return data;
    }

    /**
     * nv21 转YUV420
     * @param nv21 YYYYYYYY VUVU
     * @param width
     * @param height
     * @return
     */
    public static byte[] nv21ToYUV420(byte[] nv21, int width, int height) {
        byte[] yuv420 = new byte[(int) ((double) (width * height) * 1.5D)];

        if (nv21 != null && yuv420 != null) {
            int frameSize = width * height;

            System.arraycopy(nv21, 0, yuv420, 0, frameSize);

            for (int i = 0; i < frameSize / 4; ++i) {
                yuv420[frameSize + frameSize / 4 + i] = nv21[i * 2 + frameSize];
            }

            for (int j = 0; j < frameSize / 4; ++j) {
                yuv420[frameSize + j] = nv21[j * 2 + 1 + frameSize];
            }
        }
        return yuv420;
    }
}
