package com.qiyei.android.media.api;

import android.hardware.Camera;
import android.util.Log;

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
}
