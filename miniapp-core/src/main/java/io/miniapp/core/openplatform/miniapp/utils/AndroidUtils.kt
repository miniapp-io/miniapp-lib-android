package io.miniapp.core.openplatform.miniapp.utils

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Point
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Vibrator
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Display
import android.view.View
import android.view.ViewConfiguration
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import io.miniapp.core.R
import io.miniapp.core.openplatform.miniapp.ui.DefaultResourcesProvider
import io.miniapp.core.openplatform.miniapp.utils.Utilities.parseInt
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min


/**
 * Util class for managing themes.
 */
@SuppressLint("StaticFieldLeak")
object AndroidUtils {
    // preference key
    const val APPLICATION_THEME_KEY = "APPLICATION_THEME_KEY"

    // the theme possible values
    private const val SYSTEM_THEME_VALUE = "system"
    private const val THEME_DARK_VALUE = "dark"
    private const val THEME_LIGHT_VALUE = "light"
    private const val THEME_BLACK_VALUE = "black"

    // The default theme
    private const val DEFAULT_THEME = SYSTEM_THEME_VALUE

    private var currentTheme = AtomicReference<String>(null)

    private val mColorByAttr = HashMap<Int, Int>()

    private var readBufferLocal = ThreadLocal<ByteArray>()
    private var bufferLocal = ThreadLocal<ByteArray>()

    private lateinit var contextRef: WeakReference<Context>

    private val mContext: Context
        get() = contextRef.get()!!

    private var vibrator: Vibrator? = null

    fun getVibrator(): Vibrator? {
        if (vibrator == null) {
           vibrator = mContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        return vibrator
    }

    var density: Float = 1f
        private set

    var displaySize = Point()
        private set

    var touchSlop = 0f

    val rectTmp = RectF()
    var isInMultiWindow = false
        private set

    var statusBarHeight = 0
        private set

    var actionBarHeight = 0
        private set
        get() {
            return if (isTablet(mContext)) {
                dp(64)
            } else if (displaySize.x > displaySize.y) {
                dp(48)
            } else {
                dp(56)
            }
        }

    var navigationBarHeight = 0
        private set

    private var firstConfigurationWas: Boolean = true
        private set

    const val LIGHT_STATUS_BAR_OVERLAY = 0x0f000000
    const val DARK_STATUS_BAR_OVERLAY = 0x33000000

    fun checkAndroidTheme(context: Context, open: Boolean) {
        contextRef = WeakReference(context)
        //context.setTheme(if (isSystemDarkTheme(context.resources) && open) R.style.Theme_TMessages_Dark else R.style.Theme_TMessages_Light)
    }

    /**
     * @return true if current theme is System
     */
    fun isSystemTheme(context: Context): Boolean {
        val theme = getApplicationTheme(context)
        return theme == SYSTEM_THEME_VALUE
    }

    /**
     * @return true if current theme is Light or current theme is System and system theme is light
     */
    fun isLightTheme(): Boolean {
        val theme = getApplicationTheme(mContext)
        return theme == THEME_LIGHT_VALUE ||
                (theme == SYSTEM_THEME_VALUE && !isSystemDarkTheme(mContext.resources))
    }

    /**
     * Provides the selected application theme.
     *
     * @param context the context
     * @return the selected application theme
     */
    private fun getApplicationTheme(context: Context): String {
        val currentTheme = currentTheme.get()
        return if (currentTheme == null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            var themeFromPref = prefs.getString(APPLICATION_THEME_KEY, DEFAULT_THEME) ?: DEFAULT_THEME
            if (themeFromPref == "status") {
                // Migrate to the default theme
                themeFromPref = DEFAULT_THEME
                prefs.edit { putString(APPLICATION_THEME_KEY, DEFAULT_THEME) }
            }
            AndroidUtils.currentTheme.set(themeFromPref)
            themeFromPref
        } else {
            currentTheme
        }
    }

    /**
     * @return true if system theme is dark
     */
    private fun isSystemDarkTheme(resources: Resources): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Translates color attributes to colors.
     *
     * @param c Context
     * @param colorAttribute Color Attribute
     * @return Requested Color
     */
    @ColorInt
    fun getColor(c: Context, @AttrRes colorAttribute: Int): Int {
        return mColorByAttr.getOrPut(colorAttribute) {
            try {
                val color = TypedValue()
                c.theme.resolveAttribute(colorAttribute, color, true)
                color.data
            } catch (e: Exception) {
                Log.e("ThemeUtil", "Unable to get color")
                ContextCompat.getColor(c, android.R.color.holo_red_dark)
            }
        }
    }

    @SuppressLint("SupportAnnotationUsage", "ResourceType")
    @StringRes
    fun getString(@AttrRes strAttribute: Int): String {
        val config = Configuration(mContext.resources.configuration)
        config.setLocale(DefaultResourcesProvider.getLocale())
        return try {
            mContext.createConfigurationContext(config).getText(strAttribute).toString()
        } catch (e: Exception) {
            mContext.getString(strAttribute)
        }
    }

    fun getAttribute(c: Context, @AttrRes attribute: Int): TypedValue? {
        try {
            val typedValue = TypedValue()
            c.theme.resolveAttribute(attribute, typedValue, true)
            return typedValue
        } catch (e: Exception) {
            Log.e("ThemeUtil","Unable to get color")
        }
        return null
    }

    fun dpf2(value: Float): Float {
        return if (value == 0f) {
            0f
        } else density * value
    }

    fun dp(value: Float): Int {
        return if (value.toInt() == 0) {
            0
        } else ceil((density * value).toDouble()).toInt()
    }

    fun dp(value: Int): Int {
        return if (value == 0) {
            0
        } else ceil((density * value).toDouble()).toInt()
    }

    fun isTablet(context: Context): Boolean {
        return context.applicationContext != null &&
                context.applicationContext.resources.getBoolean(R.bool.isTablet)
    }

    fun isSmallTablet(): Boolean {
        val minSide: Float = min(
            displaySize.x,
            displaySize.y
        ) / density
        return minSide <= 690
    }

    fun fillStatusBarHeight(context: Context, force: Boolean) {
        if ( statusBarHeight > 0 && !force) {
            return
        }
        statusBarHeight = getStatusBarHeight(context)
        navigationBarHeight = getNavigationBarHeight(context)
    }

    fun getActionBarSize(context: Context): Int {
        var sizeValue = 0
        val styledAttributes: TypedArray = context.theme.obtainStyledAttributes(
            intArrayOf(android.R.attr.actionBarSize)
        )
        sizeValue = styledAttributes.getDimension(0, 0f).toInt()
        styledAttributes.recycle()
        return dp(sizeValue)
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private fun getNavigationBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun setLightNavigationBar(view: View?, enable: Boolean) {
        if (view != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = view.windowInsetsController

            if (windowInsetsController != null) {

                if (enable) {
                    windowInsetsController.setSystemBarsAppearance(
                        APPEARANCE_LIGHT_NAVIGATION_BARS,
                        APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                } else {
                    windowInsetsController.setSystemBarsAppearance(0, APPEARANCE_LIGHT_NAVIGATION_BARS)
                }
            }
        }
        else if (view != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            var flags = view.systemUiVisibility
            @Suppress("DEPRECATION")
            if (flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR > 0 != enable) {
                flags = if (enable) {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
                view.systemUiVisibility = flags
            }
        }
    }

    fun setLightNavigationBar(window: Window?, enable: Boolean) {
        if (window != null) {
            setLightNavigationBar(window.decorView, enable)
        }
    }

    fun setLightStatusBar(view: View, enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = view.windowInsetsController
            if (windowInsetsController != null) {
                if (enable) {
                    windowInsetsController.setSystemBarsAppearance(
                        APPEARANCE_LIGHT_STATUS_BARS,
                        APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    windowInsetsController.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS)
                }
            }
        } else {
            var flags = view.systemUiVisibility
            if (enable) {
                if (flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR == 0) {
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    view.systemUiVisibility = flags
                }
            } else {
                if (flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR != 0) {
                    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                    view.systemUiVisibility = flags
                }
            }
        }
    }

    fun isStatusBarLight(view: View): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = view.windowInsetsController
            if (windowInsetsController != null) {
                val barAppearance = windowInsetsController.systemBarsAppearance
                return (barAppearance and APPEARANCE_LIGHT_STATUS_BARS) != 0
            }
        } else {
            @Suppress("DEPRECATION")
            val flags = view.systemUiVisibility
            @Suppress("DEPRECATION")
            return (flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) != 0
        }
        return false
    }

    fun checkDisplaySize(context: Context, newConfiguration: Configuration?) {
        try {
            density = context.resources.displayMetrics.density
            firstConfigurationWas = true

            val configuration = newConfiguration ?: context.resources.configuration
            if (configuration.screenWidthDp != Configuration.SCREEN_WIDTH_DP_UNDEFINED) {
                val newSize =
                    ceil((configuration.screenWidthDp * density).toDouble()).toInt()
                if (abs((displaySize.x - newSize).toDouble()) > 3) {
                    displaySize.x = newSize
                }
            }
            if (configuration.screenHeightDp != Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
                val newSize =
                    ceil((configuration.screenHeightDp * density).toDouble())
                        .toInt()
                if (abs((displaySize.y - newSize).toDouble()) > 3) {
                    displaySize.y = newSize
                }
            }
            val vc = ViewConfiguration.get(context)
            touchSlop = vc.scaledTouchSlop.toFloat()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentActionBarHeight(context: Context): Int {
        return if (isTablet(context)) {
            dp(64f)
        } else if (displaySize.x > displaySize.y) {
            dp(48f)
        } else {
            dp(56f)
        }
    }

    fun getViewInset(view: View): Int {
        return if (Build.VERSION.SDK_INT >= 30) {
            WindowInsets.Type.systemBars()
        } else {
            val insets = view.getRootWindowInsets()
            insets?.stableInsetBottom ?: 0
        }
    }

    fun getStringBytes(src: String): ByteArray {
        try {
            return src.toByteArray(charset("UTF-8"))
        } catch (ignore: java.lang.Exception) {
        }
        return ByteArray(0)
    }

    fun updateImageViewImageAnimated(imageView: ImageView, newIcon: Int) {
        updateImageViewImageAnimated(
            imageView,
            ContextCompat.getDrawable(imageView.context, newIcon)
        )
    }

    fun updateImageViewImageAnimated(imageView: ImageView, newIcon: Drawable?) {
        if (imageView.getDrawable() === newIcon) {
            return
        }
        val animator = ValueAnimator.ofFloat(0f, 1f).setDuration(150)
        val changed = AtomicBoolean()
        animator.addUpdateListener { animation: ValueAnimator ->
            val `val` = animation.getAnimatedValue() as Float
            val scale = (0.5f + abs((`val` - 0.5f).toDouble())).toFloat()
            imageView.scaleX = scale
            imageView.scaleY = scale
            if (`val` >= 0.5f && !changed.get()) {
                changed.set(true)
                imageView.setImageDrawable(newIcon)
            }
        }
        animator.start()
    }

    fun hideKeyboard(view: View) {
        try {
            val imm =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (!imm.isActive()) {
                return
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun getScreenHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = wm.defaultDisplay

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = DisplayMetrics()
            display.getRealMetrics(metrics)
            metrics.heightPixels
        } else {
            val size = Point()
            display.getRealSize(size)
            size.y
        }
    }

    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = wm.defaultDisplay

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = DisplayMetrics()
            display.getRealMetrics(metrics)
            metrics.widthPixels
        } else {
            val size = Point()
            display.getRealSize(size)
            size.x
        }
    }

    val CPU_COUNT = Runtime.getRuntime().availableProcessors()

    private val LOW_SOC = intArrayOf(
        -1775228513,  // EXYNOS 850
        802464304,  // EXYNOS 7872
        802464333,  // EXYNOS 7880
        802464302,  // EXYNOS 7870
        2067362118,  // MSM8953
        2067362060,  // MSM8937
        2067362084,  // MSM8940
        2067362241,  // MSM8992
        2067362117,  // MSM8952
        2067361998,  // MSM8917
        -1853602818 // SDM439
    )

    private const val overrideDevicePerformanceClass = 0

    private var devicePerformanceClass = 0

    fun capitalizeFirst(str: String?): String {
        if (str == null) return ""
        return if (str.length <= 1) str.uppercase(Locale.getDefault()) else str.substring(0, 1)
            .uppercase(
                Locale.getDefault()
            ) + str.substring(1).lowercase(Locale.getDefault())
    }

    fun getDevicePerformanceClass(): Int {
        if (overrideDevicePerformanceClass != -1) {
            return overrideDevicePerformanceClass
        }
        if (devicePerformanceClass == -1) {
            devicePerformanceClass = measureDevicePerformanceClass()
        }
        return devicePerformanceClass
    }

    private fun measureDevicePerformanceClass(): Int {
        val androidVersion = Build.VERSION.SDK_INT
        val cpuCount: Int = CPU_COUNT
        val memoryClass =
            (mContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hash = Build.SOC_MODEL.uppercase(Locale.getDefault()).hashCode()
            for (i in LOW_SOC.indices) {
                if (LOW_SOC.get(i) === hash) {
                    return 0
                }
            }
        }
        var totalCpuFreq = 0
        var freqResolved = 0
        for (i in 0 until cpuCount) {
            try {
                val reader = RandomAccessFile(
                    String.format(
                        Locale.ENGLISH,
                        "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq",
                        i
                    ), "r"
                )
                val line = reader.readLine()
                if (line != null) {
                    totalCpuFreq += parseInt(line) / 1000
                    freqResolved++
                }
                reader.close()
            } catch (ignore: Throwable) {
            }
        }
        val maxCpuFreq =
            if (freqResolved == 0) -1 else ceil((totalCpuFreq / freqResolved.toFloat()).toDouble())
                .toInt()
        var ram: Long = -1
        try {
            val memoryInfo = ActivityManager.MemoryInfo()
            (mContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(
                memoryInfo
            )
            ram = memoryInfo.totalMem
        } catch (ignore: java.lang.Exception) {
        }
        val performanceClass: Int = if (androidVersion < 21 || cpuCount <= 2 || memoryClass <= 100 || cpuCount <= 4 && maxCpuFreq != -1 && maxCpuFreq <= 1250 || cpuCount <= 4 && maxCpuFreq <= 1600 && memoryClass <= 128 && androidVersion <= 21 || cpuCount <= 4 && maxCpuFreq <= 1300 && memoryClass <= 128 && androidVersion <= 24 || ram != -1L && ram < 2L * 1024L * 1024L * 1024L) {
            0
        } else if (cpuCount < 8 || memoryClass <= 160 || maxCpuFreq != -1 && maxCpuFreq <= 2055 || maxCpuFreq == -1 && cpuCount == 8 && androidVersion <= 23) {
            1
        } else {
            2
        }

        return performanceClass
    }

    fun readRes(rawRes: Int): String? {
        return readRes(null, rawRes)
    }

   private fun readRes(path: File?): String? {
        return readRes(path, 0)
    }

    private fun readRes(path: File?, rawRes: Int): String? {
        var totalRead = 0
        var readBuffer: ByteArray? = readBufferLocal.get()
        if (readBuffer == null) {
            readBuffer = ByteArray(64 * 1024)
            readBufferLocal.set(readBuffer)
        }
        var inputStream: InputStream? = null
        try {
            inputStream = if (path != null) {
                FileInputStream(path)
            } else {
                mContext.resources.openRawResource(rawRes)
            }
            var readLen: Int
            var buffer: ByteArray? = bufferLocal.get()
            if (buffer == null) {
                buffer = ByteArray(4096)
                bufferLocal.set(buffer)
            }
            while (inputStream.read(buffer, 0, buffer.size).also { readLen = it } >= 0) {
                if (readBuffer!!.size < totalRead + readLen) {
                    val newBuffer = ByteArray(readBuffer!!.size * 2)
                    System.arraycopy(readBuffer, 0, newBuffer, 0, totalRead)
                    readBuffer = newBuffer
                    readBufferLocal.set(readBuffer)
                }
                if (readLen > 0) {
                    System.arraycopy(buffer, 0, readBuffer, totalRead, readLen)
                    totalRead += readLen
                }
            }
        } catch (e: Throwable) {
            return null
        } finally {
            try {
                inputStream?.close()
            } catch (ignore: Throwable) {
            }
        }
        return String(readBuffer, 0, totalRead)
    }

    fun getApplicationIdUsingPackageName(): String {
        return mContext.packageName
    }
}
