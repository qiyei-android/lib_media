package com.qiyei.android.media.lib.camera.camera2;

import android.content.Context;
import android.view.OrientationEventListener;

public class DeviceOrientationListener extends OrientationEventListener {

    private int mOrientation;

    public DeviceOrientationListener(Context context) {
        super(context);
    }

    public DeviceOrientationListener(Context context, int rate) {
        super(context, rate);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        mOrientation = orientation;
    }

    public int getOrientation() {
        return mOrientation;
    }
}
