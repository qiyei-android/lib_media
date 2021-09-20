package com.qiyei.android.media.app.ui.fragment;


import android.view.View;


import com.qiyei.android.media.app.R;
import com.qiyei.android.media.app.entity.MainMenu;
import com.qiyei.android.media.app.ui.activity.Camera1DemoActivity;
import com.qiyei.android.media.app.ui.activity.Camera2DemoActivity;
import com.qiyei.android.media.app.ui.activity.H262MediaCodecDecoderDemoActivity;
import com.qiyei.android.media.app.ui.activity.H264MP4DemoActivity;
import com.qiyei.android.media.app.ui.activity.H264MediaCodecDemoActivity;
import com.qiyei.android.media.app.ui.activity.MediaMuxerDemoActivity;
import com.qiyei.android.media.app.ui.activity.MediaPlayActivity;
import com.qiyei.android.media.app.ui.activity.MediaRecordActivity;
import com.qiyei.android.media.app.ui.adapter.CommonListAdapter;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Created by qiyei2015 on 2018/8/24.
 * @version: 1.0
 * @email: 1273482124@qq.com
 * @description:
 */
public class MediaFragment extends CommonListFragment<MainMenu> {

    /**
     * 菜单item
     */
    private List<MainMenu> mMenuList = new ArrayList<>();

    private String[] names = new String[]{"测试1 相机Camera1使用","测试2 相机Camera2使用",
            "测试3 音视频的采集、编码、封包成 mp4 输出","测试4 音频播放","测试5 H264 摄像头录制视频编码成mp4文件","测试6 H264 摄像头麦克风录制音视频编码成 mp4文件",
            "测试7 解析mp4文件再合成mp4文件","测试8 H264 解码mp4文件"};
    private Class<?>[] clazzs = new Class[]{Camera1DemoActivity.class, Camera2DemoActivity.class,
            MediaRecordActivity.class, MediaPlayActivity.class, H264MediaCodecDemoActivity.class, H264MP4DemoActivity.class, MediaMuxerDemoActivity.class, H262MediaCodecDecoderDemoActivity.class};


    public MediaFragment() {
        for (int i = 0 ; i < names.length ; i++){
            MainMenu menu = new MainMenu(i+1,names[i],clazzs[i]);
            mMenuList.add(menu);
        }
    }

    @Override
    protected void initView(View view) {

    }

    @Override
    protected void initData() {
        setLiveData(mMenuList);
    }

    @Override
    protected CommonListFragment<MainMenu> getCurrentLifecycleOwner() {
        return this;
    }

    @Override
    protected CommonListAdapter getCommonListAdapter(int resId) {
        return new CommonListAdapter<MainMenu>(getContext(),mMenuList, resId) {
            @Override
            public void convert(BaseViewHolder holder, MainMenu item, int position) {
                holder.setText(R.id.tv,item.getName());
            }
        };
    }

    @Override
    protected void onItemClick(View v, MainMenu item, int position) {
        switch (item.getId()){
            default:
                startActivity(item.getClazz());
                break;
        }
    }
}
