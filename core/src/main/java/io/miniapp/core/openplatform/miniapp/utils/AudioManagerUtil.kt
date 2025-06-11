package io.miniapp.core.openplatform.miniapp.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.webkit.WebView
import androidx.annotation.RequiresApi

internal class AudioManagerUtil(context: Context) {

    private var mAudioManager : AudioManager
    private var isPause: Boolean = false

    init {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun requestAudioFocus(webView: WebView, pause: Boolean) {
        this.isPause = pause
        focusWebViewAudio(webView, pause)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            requestAudioFocusNew(webView)
//        } else {
//            requestAudioFocusOld(webView)
//        }
    }

    fun requestAudioFocusOld(webView: WebView) {

        val host = this

        mAudioManager.requestAudioFocus(
            { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        // 当音频焦点丢失时，通过 JavaScript 暂停 WebView 内的音视频
                        focusWebViewAudio(webView, host.isPause)
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        // 永久失去音频焦点
                        focusWebViewAudio(webView, host.isPause)
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // 临时失去音频焦点，可能稍后会恢复
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        // 临时失去音频焦点，但允许降低音量
                        // 例如：降低播放音量
                    }
                }
            },
            AudioManager.STREAM_MUSIC, // 音频流类型，例如媒体流
            AudioManager.AUDIOFOCUS_GAIN // 请求音频焦点的类型
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun requestAudioFocusNew(webView: WebView) {

        val host = this

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        focusWebViewAudio(webView, host.isPause)
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        focusWebViewAudio(webView, host.isPause)
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { /* 临时失去焦点时的逻辑 */ }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { /* 临时失去焦点且可降低音量 */ }
                }
            }
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()

        mAudioManager.requestAudioFocus(focusRequest)
    }

    private fun focusWebViewAudio(webView: WebView, pause: Boolean) {
        if (pause) {
            webView.evaluateJavascript("document.querySelectorAll('audio, video').forEach(el => el.pause());", null)
        } else {
            webView.evaluateJavascript("document.querySelectorAll('audio, video').forEach(el => el.play());", null)
        }
    }
}