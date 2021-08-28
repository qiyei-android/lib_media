package com.qiyei.android.media.api;

import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;

import java.util.List;

public class CameraUtils {

    private static final String TAG = "CameraUtils";


    public static int[] determineMaximumSupportedFrameRate(Camera.Parameters parameters){
        int[] fps = new int[2];
        fps[0] = Integer.MAX_VALUE;

        List<int[]> list = parameters.getSupportedPreviewFpsRange();
        for (int i = 0 ;i < list.size() ;i++){
            int min = list.get(i)[0];
            int max = list.get(i)[1];
            Log.i(TAG,"getSupportedPreviewFpsRange fps,min=" + min + " max=" + max);
            if (fps[0] > min){
                fps[0] = min;
            }
            if (fps[1] < max){
                fps[1] = max;
            }
        }
        return fps;
    }

    /**
     * 获取最佳的size
     * @param map
     * @param clazz
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public static Size getOptimalSize(StreamConfigurationMap map, Class<?> clazz, int maxWidth, int maxHeight) {
        //宽高比
        double aspectRatio = maxWidth * 1.0 / maxHeight;

        Size[] sizes = map.getOutputSizes(clazz);
        if (sizes != null){
            for (Size size : sizes){
                if ((size.getWidth() * 1.0 / size.getHeight() == aspectRatio) && size.getHeight() <= maxHeight && size.getWidth() <= maxWidth) {
                    return size;
                }
            }
        }
        return null;
    }


    public static int getDisplayRotation(WindowManager windowManager,CameraCharacteristics cameraCharacteristics) {
        //设备自然方向
        int naturalRotation = windowManager.getDefaultDisplay().getRotation();
        int degree = 0;
        int rotation = 0;
        switch (naturalRotation){
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
            default:
        }

        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
            rotation = (360 - (sensorOrientation + degree) % 360) % 360;
        } else {
            rotation = (sensorOrientation - degree + 360) % 360;
        }
        return rotation;
    }
}
