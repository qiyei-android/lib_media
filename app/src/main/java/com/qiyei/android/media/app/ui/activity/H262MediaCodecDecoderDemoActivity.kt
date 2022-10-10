package com.qiyei.android.media.app.ui.activity

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import com.qiyei.android.media.api.DecoderCallBack
import com.qiyei.android.media.api.MediaConstant
import com.qiyei.android.media.api.MediaUtils
import com.qiyei.android.media.app.R
import com.qiyei.android.media.app.extend.onClick
import com.qiyei.android.media.lib.decoder.H264VideoMediaCodecDecoder
import kotlinx.android.synthetic.main.activity_h262_media_codec_decoder_demo.*
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * H264 Decoder Demo
 */
class H262MediaCodecDecoderDemoActivity : AppCompatActivity() {


    lateinit var mH264MediaCodecDecoder:H264VideoMediaCodecDecoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_h262_media_codec_decoder_demo)

        decoder_preview.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                getMediaFile()?.let {
                    mH264MediaCodecDecoder = H264VideoMediaCodecDecoder(Surface(surface),width,height,it)
                }

                mH264MediaCodecDecoder.setCallBack(object :DecoderCallBack{
                    override fun outputMediaFormatChanged(type: Int, mediaFormat: MediaFormat?) {

                    }

                    override fun onDecodeOutput(type: Int, byteBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo?) {
                        if (byteBuffer != null) {
                            Log.i(MediaConstant.H264_TAG,"H262MediaCodecDecoderDemoActivity onDecodeOutput,byteBuffer,size=" +
                                    byteBuffer.remaining() + " bufferInfo=" + bufferInfo)
                        }
                    }

                    override fun onStop(type: Int) {

                    }
                })
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }
        }
        btn1.onClick {
            mH264MediaCodecDecoder.start()
        }
    }

    private fun getMediaFile():String? {
        val fileDir = File(MediaUtils.getMediaStorePath())
        try {
            if (fileDir.isDirectory) {
                for (file in fileDir.listFiles()) {
                    if (!file.name.contains("reMuxter_")) {
                        return file.canonicalPath;
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}