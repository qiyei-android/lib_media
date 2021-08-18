package com.qiyei.android.media.app.ui.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.qiyei.android.media.app.R;
import com.qiyei.android.media.app.entity.MainMenu;
import com.qiyei.android.media.app.listener.CommonListClickListener;
import com.qiyei.android.media.app.ui.adapter.CommonListAdapter;
import com.qiyei.android.media.app.ui.view.CategoryItemDecoration;
import com.qiyei.android.media.app.vm.CommonListViewModel;

import java.util.List;

/**
 * @author Created by qiyei2015 on 2018/8/26.
 * @version: 1.0
 * @email: 1273482124@qq.com
 * @description: 公共的列表Fragment
 */
public abstract class CommonListFragment<T> extends Fragment {

    private RecyclerView mRecyclerView;

    /**
     * ViewModel
     */
    private CommonListViewModel<T> mMenuViewModel;

    private CommonListAdapter<T> mMenuAdapter;

    public CommonListFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(getTAG(),"onCreateView");
        View contentView = inflater.inflate(R.layout.fragment_common_list, container, false);
        init(contentView);
        initView(contentView);
        initData();
        return contentView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    protected String getTAG() {
        return "CommonListFragment";
    }

    /**
     * View初始化
     * @param view
     */
    protected abstract void initView(View view);

    /**
     * 数据绑定及初始化
     */
    protected abstract void initData();

    protected abstract void onItemClick(View v, T item, int position);

    protected abstract CommonListAdapter getCommonListAdapter(int resId);

    protected abstract CommonListFragment<T> getCurrentLifecycleOwner();

    /**
     * 初始化
     * @param view
     */
    private void init(View view){
        //初始化RV
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mRecyclerView.addItemDecoration(new CategoryItemDecoration(getContext().getDrawable(R.drawable.recyclerview_decoration)));
        //初始化Adapter
        mMenuAdapter = getCommonListAdapter(R.layout.recyclerview_main_menu_item);

        mMenuAdapter.setItemOnClickListener(new MyListener());
        //初始化ViewModel
        mMenuViewModel = new ViewModelProvider(this).get(CommonListViewModel.class);

        mMenuViewModel.getLiveData().observe(getCurrentLifecycleOwner().getViewLifecycleOwner(), new Observer<List<T>>() {
            @Override
            public void onChanged(@Nullable List<T> mainMenus) {
                //update UI
                mMenuAdapter.setDatas(mainMenus);
            }
        });

        mRecyclerView.setAdapter(mMenuAdapter);
    }

    /**
     * 设置显示的数据
     */
    protected void setLiveData(List<T> list){
        //主动更新数据
        mMenuViewModel.getLiveData().setValue(list);
    }

    protected void startActivity(Class<?> clazz){
        if (getContext() != null){
            Log.i(getTAG(),"startActivity,clazz:" + clazz);
            startActivity(new Intent(getContext(),clazz));
            return;
        }
        Log.i(getTAG(),"startActivity failed,mContext is null,clazz:" + clazz);
    }

    private class MyListener implements CommonListClickListener<T> {

        @Override
        public void onClick(View v, T item, int position) {
            onItemClick(v,item,position);
        }
    }

}
