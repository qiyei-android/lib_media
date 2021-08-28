package com.qiyei.android.media.lib.camera.camera2;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;

public class CameraInfo {
    public String cameraId;
    public CameraCharacteristics characteristics;
    public Integer level;
    public Integer facing;
    public StreamConfigurationMap map;

    @Override
    public String toString() {
        return "CameraInfo{" +
                "cameraId='" + cameraId + '\'' +
                ", characteristics=" + characteristics +
                ", level=" + level +
                ", facing=" + facing +
                ", map=" + map +
                '}';
    }
}
