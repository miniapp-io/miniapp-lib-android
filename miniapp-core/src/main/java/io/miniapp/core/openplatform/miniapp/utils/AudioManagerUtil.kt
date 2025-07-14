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
                        // When audio focus is lost, pause audio/video in WebView via JavaScript
                        focusWebViewAudio(webView, host.isPause)
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        // Permanently lose audio focus
                        focusWebViewAudio(webView, host.isPause)
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // Temporarily lose audio focus, may recover later
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        // Temporarily lose audio focus, but allow volume reduction
                        // For example: reduce playback volume
                    }
                }
            },
            AudioManager.STREAM_MUSIC, // Audio stream type, e.g. media stream
            AudioManager.AUDIOFOCUS_GAIN // Type of audio focus request
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
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { /* Logic when temporarily losing focus */ }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { /* Temporarily lose focus and can reduce volume */ }
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