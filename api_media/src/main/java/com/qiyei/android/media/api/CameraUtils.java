package com.qiyei.android.media.api;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

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

    /**
     * 获取图像矫正角度
     * @param windowManager
     * @param cameraCharacteristics
     * @return
     */
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

    /**
     * 获取image的角度
     * @return
     */
    public static int getImageOrientation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) {
            return 0;
        }

        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int orientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) {
            orientation = -orientation;
        }

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + orientation + 360) % 360;
    }

    /**
     * 获取系统位置
     * @param context
     * @return
     */
    public static Location getLocation(Context context){
        LocationManager locationManager =(LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }
        return null;
    }

    /**
     * 写入系统媒体库
     * @param tag
     * @param context
     * @param captureResult
     * @param buffer
     * @param width
     * @param height
     */
    public static void writeToMediaStore(String tag,Context context, CaptureResult captureResult,byte[] buffer, int width, int height){
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
        String cameraDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator +  "Camera";

        long date = System.currentTimeMillis();
        //e.g. IMG_20190211100833786
        String title = "IMG_" + dateFormat.format(date);
        // e.g. IMG_20190211100833786.jpeg
        String displayName = title + ".jpeg";;//
        ///sdcard/DCIM/Camera/IMG_20190211100833786.jpeg
        String path = cameraDir + File.separator + displayName;
        int orientation = captureResult.get(CaptureResult.JPEG_ORIENTATION);
        Location location = captureResult.get(CaptureResult.JPEG_GPS_LOCATION);
        double longitude = 0;
        double latitude = 0;
        if (location != null){
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        // Write the jpeg data into the specified file.
        try (FileOutputStream fs = new FileOutputStream(path)) {

            fs.write(buffer);
            Log.i(tag,"write MediaStore successful,path=" + path);
            // Insert the image information into the media store.
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.ImageColumns.TITLE, title);
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName);
            values.put(MediaStore.Images.ImageColumns.DATA, path);
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date);
            values.put(MediaStore.Images.ImageColumns.WIDTH, width);
            values.put(MediaStore.Images.ImageColumns.HEIGHT, height);
            values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation);
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, longitude);
            values.put(MediaStore.Images.ImageColumns.LATITUDE, latitude);
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Log.i(tag,"insert MediaStore successful,uri=" + uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
