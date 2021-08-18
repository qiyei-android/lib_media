package com.qiyei.android.media.app.listener;

import android.view.View;

import com.qiyei.android.media.app.entity.MainMenu;


/**
 * @author Created by qiyei2015 on 2018/8/24.
 * @version: 1.0
 * @email: 1273482124@qq.com
 * @description:
 */
public interface CommonListClickListener<T> {

    /**
     * 点击事件
     * @param v
     * @param item
     * @param position
     */
    void onClick(View v , T item, int position);

}
