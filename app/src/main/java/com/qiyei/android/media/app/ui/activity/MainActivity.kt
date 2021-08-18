package com.qiyei.android.media.app.ui.activity


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.qiyei.android.media.app.R
import com.qiyei.android.media.app.ui.fragment.MediaFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val fragment = MediaFragment()
        // 开启事物
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        // 第一个参数是Fragment的容器id，需要添加的Fragment
        fragmentTransaction.add(R.id.content_layout, fragment)
        // 一定要commit
        fragmentTransaction.commit()
    }
}