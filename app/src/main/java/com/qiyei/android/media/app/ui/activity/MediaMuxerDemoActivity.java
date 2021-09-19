package com.qiyei.android.media.app.ui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.qiyei.android.media.api.MediaUtils;
import com.qiyei.android.media.app.R;
import com.qiyei.android.media.lib.decoder.CustomMediaMuxer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MediaMuxerDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_muxer_demo);
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testMediaMuxer();
            }
        });
    }

    private void testMediaMuxer(){
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
        String outPath = MediaUtils.getMediaStorePath() + File.separator + "reMuxter_" + dateFormat.format(System.currentTimeMillis()) + ".mp4";

        File fileDir = new File(MediaUtils.getMediaStorePath());
        try {
            if (fileDir.isDirectory()){
                for (File file : fileDir.listFiles()){
                    if (!file.getName().contains("reMuxter_")){
                        CustomMediaMuxer mediaMuxer = new CustomMediaMuxer(file.getCanonicalPath(),outPath);
                        mediaMuxer.start();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}