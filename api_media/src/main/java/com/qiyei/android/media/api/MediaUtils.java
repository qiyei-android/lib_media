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

        Image.Plane yPlane = planes[0];

        byte[] rowData = new byte[yPlane.getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        int i = 0;


        for(int size = planes.length; i < size; ++i) {
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

            yPlane = planes[i];

            ByteBuffer buffer = yPlane.getBuffer();

            yPlane = planes[i];

            int rowStride = yPlane.getRowStride();
            yPlane = planes[i];

            int pixelStride = yPlane.getPixelStride();
            int shift = i == 0 ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            int row = 0;

            for(int j = h; row < j; ++row) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, w);
                    channelOffset += w;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    int col = 0;

                    for(int var22 = w; col < var22; ++col) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }

                if (row < h - 1) {
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
