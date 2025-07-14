package io.miniapp.core.openplatform.miniapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import io.miniapp.core.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object HomeScreenShortcutUtils {

    private const val ACTION_OW3_APP_SHORTCUT = "ACTION_OW3_APP_SHORTCUT"

    const val SHORTCUT_MINIAPP_ID_BEGIN = "miniappx_miniapp_shortcut_"
    const val SHORTCUT_DAPP_ID_BEGIN = "miniappx_dapp_shortcut_"

    const val SHORTCUT_LINK = "miniappx_link"
    const val SHORTCUT_TYPE = "miniappx_type"
    const val SHORTCUT_DATA = "miniappx_data"

    const val SHORTCUT_MINIAPP = "MINIAPP"
    const val SHORTCUT_DAPP = "WEBPAGE"

    var launchScheme: String? = null

    private val directShareCategory = AndroidUtils.getApplicationIdUsingPackageName() + ".SHORTCUT_SHARE"

    private fun getAppLaunchIntent(context: Context, link: String, type: String,  data: String?): Intent? {
        try {
            return if (launchScheme != null) {
                val deepLink = Uri.parse(launchScheme)
                val scheme = deepLink.scheme
                val host = deepLink.host
                val uri = Uri.Builder()
                    .scheme(scheme)
                    .authority(host)
                    .appendQueryParameter(SHORTCUT_LINK, link)
                    .appendQueryParameter(SHORTCUT_TYPE, type)
                    .appendQueryParameter(SHORTCUT_DATA, data)
                    .build()
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            } else {
                val packageManager = context.packageManager
                packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    putExtra(SHORTCUT_LINK, link)
                    putExtra(SHORTCUT_TYPE, type)
                    putExtra(SHORTCUT_DATA, data)
                    action = ACTION_OW3_APP_SHORTCUT
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addDynamicShortcut(context: Context, link: String, label: String, id:  String, type: String, data: String?, bitmap: Bitmap?) {
        getAppLaunchIntent(context, link, type, data)?.also {

            val categories = mutableSetOf<String>()
            categories.add(directShareCategory)
            if (Build.VERSION.SDK_INT >= 25) {
                categories.add(ShortcutInfo.SHORTCUT_CATEGORY_CONVERSATION)
            }

            ShortcutInfoCompat.Builder(context, id)
                .setShortLabel(label)
                .setLongLabel(label)
                .setIcon( bitmap?.let { IconCompat.createWithBitmap(bitmap) } ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(it)
                .setCategories(categories)
                .build()
                .also { shortcut ->
                    ShortcutManagerCompat.requestPinShortcut(context.applicationContext, shortcut, null)
                }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun createShortcut(context: Context, link: String, label: String, type: String, data: String?, bitmap: Bitmap?) {
        getAppLaunchIntent(context, link, type, data)?.also {

            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val shortcutIcon = bitmap ?: (context.resources.getDrawable(R.mipmap.ic_launcher, null) as BitmapDrawable).bitmap

            val addShortcutIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_NAME, label)
                putExtra(Intent.EXTRA_SHORTCUT_ICON, shortcutIcon)
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, it)
                putExtra("duplicate", false)
            }

            context.sendBroadcast(addShortcutIntent)
        }
    }

    private suspend fun getBitmapFromUrl(context: Context, imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                    .get()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun isShortcutAdded(context: Context, id: String, type: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutId = if (type == SHORTCUT_DAPP) "$SHORTCUT_DAPP_ID_BEGIN${id}" else "$SHORTCUT_MINIAPP_ID_BEGIN${id}"
            val shortcuts = ShortcutManagerCompat.getShortcuts(
                context.applicationContext,
                ShortcutManagerCompat.FLAG_MATCH_PINNED
            )
            for (i in shortcuts.indices) {
                if (shortcuts[i].id == shortcutId) {
                    return true
                }
            }
        }
        return false
    }

    suspend fun createShortcutLink(activity: Context, link: String, label: String, id: String, type: String, data: String?, icon: String?) {

        val shortcutId = if (type == SHORTCUT_DAPP) "$SHORTCUT_DAPP_ID_BEGIN${id}" else "$SHORTCUT_MINIAPP_ID_BEGIN${id}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addDynamicShortcut(activity, link, label, shortcutId, type, data, bitmap = icon?.let { getBitmapFromUrl(activity, it) })
        } else {
            createShortcut(activity, link, label, type, data, bitmap = icon?.let { getBitmapFromUrl(activity, it) })
        }
    }
}


