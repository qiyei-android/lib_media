package com.qiyei.android.media.app.ui.activity

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.qiyei.android.media.app.R
import com.qiyei.android.media.app.extend.onClick


class MediaPlayActivity : AppCompatActivity() {

    lateinit var player:MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_play)

        player = MediaPlayer.create(this,R.raw.test_music)

        findViewById<Button>(R.id.btn).onClick {
            player.start()
        }
    }
}