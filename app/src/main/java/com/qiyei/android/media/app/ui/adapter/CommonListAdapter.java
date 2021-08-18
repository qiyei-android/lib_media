package com.qiyei.android.media.app.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.recyclerview.widget.RecyclerView;

import com.qiyei.android.media.app.R;
import com.qiyei.android.media.app.entity.MainMenu;
import com.qiyei.android.media.app.listener.CommonListClickListener;

import java.util.List;

/**
 * @author Created by qiyei2015 on 2018/8/18.
 * @version: 1.0
 * @email: 1273482124@qq.com
 * @description:
 */
public abstract class CommonListAdapter<T> extends RecyclerView.Adapter<CommonListAdapter.BaseViewHolder> {

    /**
     * 调试用TAG
     */
    protected static final String TAG = "MainMenuAdapter";
    /**
     * context
     */
    protected Context mContext;
    /**
     * 数据集合
     */
    protected List<T> mDatas;
    /**
     * item的布局文件
     */
    protected int mLayoutId;

    private CommonListClickListener<T> mItemOnClickListener;

    public CommonListAdapter(Context context, List<T> datas, int layoutId) {
        mContext = context;
        mDatas = datas;
        mLayoutId = layoutId;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false);
        BaseViewHolder holder = new BaseViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        if (mItemOnClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mItemOnClickListener.onClick(v, mDatas.get(position), position);
                }
            });
        }
        convert(holder, mDatas.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }


    protected String getTAG() {
        return this.getClass().getSimpleName();
    }


    /**
     * 设置数据
     *
     * @param datas
     */
    public void setDatas(List<T> datas) {
        mDatas = datas;
        notifyDataSetChanged();
    }

    /**
     * @return {@link #mDatas}
     */
    public List<T> getDatas() {
        return mDatas;
    }

    /**
     * 数据绑定操作
     *
     * @param holder
     * @param item
     * @param position
     */
    public abstract void convert(BaseViewHolder holder, T item, int position);

    /**
     * @param listener the {@link #mItemOnClickListener} to set
     */
    public void setItemOnClickListener(CommonListClickListener<T> listener) {
        mItemOnClickListener = listener;
    }

    public void removeItemOnClickListener() {
        mItemOnClickListener = null;
    }


    public static class BaseViewHolder extends RecyclerView.ViewHolder {
        /**
         * 调试用TAG
         */
        protected static final String TAG = BaseViewHolder.class.getSimpleName();

        /**
         * 缓存View
         */
        protected SparseArray<View> mViews;

        public BaseViewHolder(View itemView) {
            super(itemView);
            mViews = new SparseArray<>();
        }

        /**
         * 获取对应的View
         *
         * @param viewId
         * @param <T>
         * @return
         */
        public <T extends View> T getView(int viewId) {
            View view = mViews.get(viewId);
            //没有就从itemView 中查找
            if (view == null) {
                view = itemView.findViewById(viewId);
                mViews.put(viewId, view);
            }
            if (view == null) {
                throw new NullPointerException("getView: " + viewId + "is null , please check !");
            }
            return (T) view;
        }

        /**
         * 设置文本内容
         *
         * @param viewId
         * @param text
         */
        public BaseViewHolder setText(int viewId, CharSequence text) {
            TextView textView = getView(viewId);
            textView.setText(text);
            return this;
        }

        /**
         * 设置文本内容
         *
         * @param viewId
         * @param resId
         */
        public BaseViewHolder setText(int viewId, int resId) {
            TextView textView = getView(viewId);
            textView.setText(resId);
            return this;
        }

        /**
         * 设置文本内容
         *
         * @param viewId
         * @param color
         */
        public BaseViewHolder setTextColor(int viewId, int color) {
            TextView textView = getView(viewId);
            textView.setTextColor(color);
            return this;
        }

        /**
         * 设置文本内容
         *
         * @param viewId
         * @param size
         */
        public BaseViewHolder setTextSize(int viewId, int size) {
            TextView textView = getView(viewId);
            textView.setTextSize(size);
            return this;
        }

        /**
         * 设置View的Visibility
         *
         * @param visibility
         * @param viewId
         * @return
         */
        public BaseViewHolder setVisibility(int visibility, int viewId) {
            getView(viewId).setVisibility(visibility);
            return this;
        }

        /**
         * 设置View的Visibility
         *
         * @param visibility
         * @param args
         * @return
         */
        public BaseViewHolder setVisibility(int visibility, int... args) {
            for (int i = 0; i < args.length; i++) {
                getView(args[i]).setVisibility(visibility);
            }
            return this;
        }

        /**
         * 设置ImageView的资源
         *
         * @param viewId
         * @param resourceId
         * @return
         */
        public BaseViewHolder setImageResource(int viewId, int resourceId) {
            ImageView imageView = getView(viewId);
            imageView.setImageResource(resourceId);
            return this;
        }

        /**
         * 设置ImageView的图片
         *
         * @param viewId
         * @param bitmap
         * @return
         */
        public BaseViewHolder setImageBitmap(int viewId, Bitmap bitmap) {
            ImageView imageView = getView(viewId);
            imageView.setImageBitmap(bitmap);
            return this;
        }

        /**
         * 设置image的url
         *
         * @param viewId
         * @param url
         */
        public void setImageUrl(int viewId, String url) {
            ImageView imageView = getView(viewId);
            //加载图片
            //ImageManager.getInstance().loadImage(imageView,url);
        }

        /**
         * 设置item中某个view的点击事件
         *
         * @param viewId
         * @param listener
         */
        public void setOnClickListener(int viewId, View.OnClickListener listener) {
            getView(viewId).setOnClickListener(listener);
        }

        /**
         * 设置item中某个view的长点击事件
         *
         * @param viewId
         * @param listener
         */
        public void setOnLongClickListener(int viewId, View.OnLongClickListener listener) {
            getView(viewId).setOnLongClickListener(listener);
        }

        /**
         * 设置View可见
         *
         * @param id
         */
        public void setViewVisible(int id) {
            getView(id).setVisibility(View.VISIBLE);
        }

        /**
         * 设置View Gone
         *
         * @param id
         */
        public void setViewGone(int id) {
            getView(id).setVisibility(View.GONE);
        }

//    /**
//     * 设置图片加载url,这里稍微处理得复杂一些，因为考虑加载图片的第三方可能不太一样
//     * 也可以直接写死
//     * @param viewId
//     * @param imageLoader
//     * @return
//     */
//    public BaseViewHolder setImageByUrl(int viewId, HolderImageLoader imageLoader) {
//        ImageView imageView = getView(viewId);
//        if (imageLoader == null) {
//            throw new NullPointerException("imageLoader is null!");
//        }
//        imageLoader.displayImage(imageView.getContext(), imageView, imageLoader.getImagePath());
//        return this;
//    }
//
//    /**
//     * 图片加载，这里稍微处理得复杂一些，因为考虑加载图片的第三方可能不太一样
//     * 也可以不写这个类
//     */
//    public static abstract class HolderImageLoader {
//        private String mImagePath;
//
//        public HolderImageLoader(String imagePath) {
//            this.mImagePath = imagePath;
//        }
//
//        public String getImagePath() {
//            return mImagePath;
//        }
//
//        public abstract void displayImage(Context context, ImageView imageView, String imagePath);
//    }

    }

}
