package com.qiyei.android.media.api;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {HardwareStatus.DEFAULT, HardwareStatus.OPEN, HardwareStatus.CLOSE,
        HardwareStatus.RUNNING, HardwareStatus.IDLE})
public @interface HardwareStatus {
    int DEFAULT = 0;
    int OPEN = 1;
    int RUNNING = 2;
    int IDLE = 3;
    int CLOSE = 4;
}
