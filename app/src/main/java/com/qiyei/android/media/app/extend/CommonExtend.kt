package com.qiyei.android.media.app.extend


import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView

/**
 * @author Created by qiyei2015 on 2018/9/22.
 * @version: 1.0
 * @email: 1273482124@qq.com
 * @description: 用于kotlin 公共的扩展方法
 */




/**
 * Button使能
 */
fun Button.enable(editText: EditText,method:() -> Boolean){
    editText.addTextChangedListener(object :TextWatcher{
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            this@enable.isEnabled = method()
        }
        override fun afterTextChanged(s: Editable?) {

        }
    })
}

/**
 * 扩展点击事件
 */
fun View.onClick(listener: View.OnClickListener): View {
    setOnClickListener(listener)
    return this
}

/**
 * 扩展点击事件，参数为方法
 */
fun View.onClick(method:() -> Unit): View {
    setOnClickListener { method() }
    return this
}
