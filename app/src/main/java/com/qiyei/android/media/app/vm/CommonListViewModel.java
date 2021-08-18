package com.qiyei.android.media.app.vm;


import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import com.qiyei.android.media.app.entity.MainMenu;

import java.util.List;

/**
 * @author Created by qiyei2015 on 2018/8/18.
 * @version: 1.0
 * @email: 1273482124@qq.com
 * @description: 公共列表的ViewModel
 */
public class CommonListViewModel<T> extends ViewModel {

    /**
     * LiveData
     */
    protected MutableLiveData<List<T>> mLiveData;

    public CommonListViewModel() {
        mLiveData = new MutableLiveData<>();
    }

    /**
     * @return {@link #mLiveData}
     */
    public MutableLiveData<List<T>> getLiveData() {
        return mLiveData;
    }

}
