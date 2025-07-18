package io.miniappx.sample

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.LifecycleOwner
import io.miniapp.core.openplatform.miniapp.WebAppPreloadParameters
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniappx.sample.ui.main.ChatFragment
import io.miniappx.sample.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window = window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window!!.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        } else
            window!!.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        val params = window.attributes
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.dimAmount = 0f
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.setAttributes(params)
        window.statusBarColor = Color.TRANSPARENT

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AndroidUtils.setLightStatusBar(
                window.decorView,
                true
            )
        }

        setContentView(R.layout.activity_chat)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ChatFragment.newInstance())
                .commitNow()
        }

        preloadApps(this, this)
    }

    private fun preloadApps(owner: LifecycleOwner, context: Context) {
        listOf("2s8A4zgsdwfLgUsFoMdg8kaW73b").forEach {
            val config = WebAppPreloadParameters.Builder()
                .owner(owner)
                .context(context)
                .miniAppId(it)
                .build()

            MainScope().launch {
                miniAppService.preload( config = config)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}