package com.qiyei.android.media.lib.camera.camera2;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Range;

public class CameraInfo {
    public String cameraId;
    public CameraCharacteristics characteristics;
    public Integer level;
    public Integer facing;
    public StreamConfigurationMap map;
    public Range<Integer> mFpsRange;

    @Override
    public String toString() {
        return "CameraInfo{" +
                "cameraId='" + cameraId + '\'' +
                ", characteristics=" + characteristics +
                ", level=" + level +
                ", facing=" + facing +
                ", map=" + map +
                ", fpsRange=" + mFpsRange +
                '}';
    }
}
