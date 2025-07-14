package io.miniapp.core.openplatform.miniapp.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.Interpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import io.miniapp.core.openplatform.miniapp.IResourcesProvider
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.utils.LayoutHelper
import io.miniapp.core.openplatform.miniapp.utils.ThemeUtils

internal open class ActionBar(context: Context, private val resourcesProvider: IResourcesProvider, val builder: (ViewGroup) -> View) : FrameLayout(context) {

    interface ActionBarMenuOnItemClick {
        fun onItemClick(id: Int)
        open fun canOpenMenu(): Boolean {
            return true
        }
    }

    var actionBarMenuOnItemClick: ActionBarMenuOnItemClick? = null

    private var backButtonImageView: ImageView? = null
    private var avatarSearchImageView: ImageView? = null
    private var backButtonDrawable: Drawable? = null
    private val titleTextView: Array<SimpleTextView?> = arrayOfNulls(2)
    private var subtitleTextView: SimpleTextView? = null
    private var additionalSubtitleTextView: SimpleTextView? = null
    private var actionBarColor = -0x1
    private var isMenuOffsetSuppressed = false
    private var ignoreLayoutRequest = false
    private var occupyStatusBarVisible = true
    private val actionMode: ActionBarMenu? = null
    private var addToContainer = true
    private var clipContent = false
    private var interceptTouches = true
    private var forceSkipTouches = false
    private var extraHeight = 0
    private var supportsHolidayImage = false
    private var fontMetricsInt: FontMetricsInt? = null
    private var rect: Rect? = null
    private var titleRightMargin = 0
    private var allowOverlayTitle = false
    private var lastTitle: CharSequence? = null
    private var lastRightDrawable: Drawable? = null
    private var rightDrawableOnClickListener: OnClickListener? = null
    private var lastOverlayTitle: CharSequence? = null
    private val overlayTitleToSet = arrayOfNulls<Any>(3)
    private var lastRunnable: Runnable? = null
    private var titleOverlayShown = false
    private var titleActionRunnable: Runnable? = null
    private var isSearchFieldVisible = false
    private var searchFieldVisibleAlpha = 0f
    private var useWechatMenuStyle = false
    var itemsBackgroundColor = 0
        private set
    var itemsActionModeBackgroundColor = 0
        private set
    var itemsColor = Color.parseColor("#676A6F")
        private set
    var itemsActionModeColor = 0
        private set
    private var parentFragment: Fragment? = null
    private var titleColorToSet = 0
    private var overlayTitleAnimation = false
    private var titleAnimationRunning = false
    private var fromBottom = false
    private var centerScale = false
    private var subtitle: CharSequence? = null
    private var drawBackButton = false
    private var attached = false
    private var resumed = false
    private var attachState = false
    private var titlesContainer: FrameLayout? = null
    private var useContainerForTitles = false
    private var interceptTouchEventListener: OnTouchListener? = null
    private var contentView: SizeNotifierFrameLayout? = null
    private var blurredBackground = false
    private var blurScrimPaint = Paint()
    private var rectTmp = Rect()
   val canClickItem: Boolean
        get() {
            if ((alpha+0.01f)>1.0f) {
                return true
            }
            return false
        }

    var menu: ActionBarMenu? = null

    var ellipsizeSpanAnimator = EllipsizeSpanAnimator(this)

    private fun createBackButtonImage() {
        if (backButtonImageView != null) {
            return
        }
        backButtonImageView = ImageView(context)
        backButtonImageView!!.setScaleType(ImageView.ScaleType.CENTER)
        backButtonImageView!!.setPadding(AndroidUtils.dp(1), 0, 0, 0)
        addView(backButtonImageView, LayoutHelper.createFrame(54, 54, Gravity.LEFT or Gravity.TOP))
        backButtonImageView!!.setOnClickListener { v: View? ->
            if (canClickItem) {
                actionBarMenuOnItemClick?.onItemClick(-1)
            }
        }
    }

    fun getBackButtonDrawable(): Drawable? {
        return backButtonDrawable
    }

    fun getBackButton(): ImageView {
        return backButtonImageView!!
    }

    fun setBackButtonDrawable(drawable: Drawable?) {
        if (backButtonImageView == null) {
            createBackButtonImage()
        }
        backButtonImageView!!.setVisibility(if (drawable == null) GONE else VISIBLE)
        backButtonImageView!!.setImageDrawable(drawable.also { backButtonDrawable = it })
        if (drawable is BackDrawable) {
            drawable.setRotation(0f, false)
            drawable.setRotatedColor(itemsActionModeColor)
            drawable.setColor(itemsColor)
        }
        if (drawable is BitmapDrawable) {
            backButtonImageView!!.colorFilter = PorterDuffColorFilter(itemsColor, PorterDuff.Mode.SRC_IN)
        }
    }

    fun setBackButtonContentDescription(description: CharSequence?) {
        if (backButtonImageView != null) {
            backButtonImageView!!.setContentDescription(description)
        }
    }

    fun setSupportsHolidayImage(value: Boolean) {
        supportsHolidayImage = value
        if (supportsHolidayImage) {
            fontMetricsInt = FontMetricsInt()
            rect = Rect()
        }
        invalidate()
    }

    var searchAvatarImageView: ImageView?
        get() = avatarSearchImageView
        set(backupImageView) {
            if (avatarSearchImageView === backupImageView) {
                return
            }
            if (avatarSearchImageView != null) {
                removeView(avatarSearchImageView)
            }
            avatarSearchImageView = backupImageView
            if (avatarSearchImageView != null) {
                addView(avatarSearchImageView)
            }
        }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return interceptTouchEventListener != null && interceptTouchEventListener!!.onTouch(
            this,
            ev
        ) || super.onInterceptTouchEvent(ev)
    }

    protected fun shouldClipChild(child: View): Boolean {
        return clipContent && (child === titleTextView[0] || child === titleTextView[1] || child === subtitleTextView || child === backButtonImageView || child === additionalSubtitleTextView || child === titlesContainer)
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        if (drawBackButton && child === backButtonImageView) {
            return true
        }
        val clip = shouldClipChild(child)
        if (clip) {
            canvas.save()
            canvas.clipRect(0f, -translationY + if (occupyStatusBarVisible) AndroidUtils.statusBarHeight else 0, measuredWidth.toFloat(), measuredHeight.toFloat())
        }
        val result = super.drawChild(canvas, child, drawingTime)

        if (clip) {
            canvas.restore()
        }
        return result
    }

    override fun setTranslationY(translationY: Float) {
        super.setTranslationY(translationY)
        if (clipContent) {
            invalidate()
        }
    }

    fun setBackButtonImage(resource: Int) {
        if (backButtonImageView == null) {
            createBackButtonImage()
        }
        backButtonImageView!!.setVisibility(if (resource == 0) GONE else VISIBLE)
        backButtonImageView!!.setImageResource(resource)
        backButtonImageView!!.colorFilter = PorterDuffColorFilter(itemsColor, PorterDuff.Mode.SRC_IN)
    }

    private fun createSubtitleTextView() {
        if (subtitleTextView != null) {
            return
        }
        subtitleTextView = SimpleTextView(context).apply {
            setGravity(Gravity.LEFT)
            setMinusWidth(AndroidUtils.dp(120))
            visibility = GONE
            setContentColor(resourcesProvider.getColor("subtitle_text_color"))
        }

        addView(
            subtitleTextView,
            0,
            LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.LEFT or Gravity.TOP
            )
        )
    }

    fun createAdditionalSubtitleTextView() {
        if (additionalSubtitleTextView != null) {
            return
        }
        additionalSubtitleTextView = SimpleTextView(context).apply {
            setGravity(Gravity.LEFT)
            visibility = GONE
            setContentColor(resourcesProvider.getColor("destructive_text_color"))
        }

        addView(
            additionalSubtitleTextView,
            0,
            LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.LEFT or Gravity.TOP
            )
        )
    }

    fun getAdditionalSubtitleTextView(): SimpleTextView? {
        return additionalSubtitleTextView
    }

    fun setAddToContainer(value: Boolean) {
        addToContainer = value
    }

    fun shouldAddToContainer(): Boolean {
        return addToContainer
    }

    fun setClipContent(value: Boolean) {
        clipContent = value
    }

    fun setSubtitle(value: CharSequence?) {
        if (value != null && subtitleTextView == null) {
            createSubtitleTextView()
        }

        subtitleTextView?.apply {
            val isEmpty = TextUtils.isEmpty(value)
            visibility = if (!isEmpty && !isSearchFieldVisible) VISIBLE else GONE
            setAlpha(1f)
            if (!isEmpty) {
                setText(value)
            }
            subtitle = value
        }
    }

   fun updateItemTextColor() {
        setTitleColor(resourcesProvider.getColor("text_color"))
        if (subtitleTextView != null) {
            subtitleTextView!!.setContentColor(resourcesProvider.getColor("subtitle_text_color"))
        }
        setItemsColor(resourcesProvider.getColor("text_color"), false)
        setItemsBackgroundColor(resourcesProvider.getColor("section_bg_color"), false)
    }

    private fun createTitleTextView(i: Int) {
        if (titleTextView[i] != null) {
            return
        }
        titleTextView[i] = SimpleTextView(context).also {
            it.setMinusWidth(AndroidUtils.dp(120))
            it.setGravity(Gravity.LEFT or Gravity.CENTER_VERTICAL)
            if (titleColorToSet != 0) {
                it.setContentColor(titleColorToSet)
            } else {
                it.setContentColor(resourcesProvider.getColor("text_color"))
            }
            it.setDrawablePadding(AndroidUtils.dp(4))
            it.setRightDrawableTopPadding(-AndroidUtils.dp(1))
        }

        if (useContainerForTitles) {
            titlesContainer?.addView(
                titleTextView[i],
                0,
                LayoutHelper.createFrameWithSize(
                    LayoutHelper.WRAP_CONTENT,
                    getCurrentActionBarHeight(context),
                    Gravity.LEFT or Gravity.BOTTOM
                )
            )
        } else {
            addView(
                titleTextView[i],
                0,
                LayoutHelper.createFrameWithSize(
                    LayoutHelper.WRAP_CONTENT,
                    getCurrentActionBarHeight(context),
                    Gravity.LEFT or Gravity.BOTTOM
                )
            )
        }
    }

    fun setTitleRightMargin(value: Int) {
        titleRightMargin = value
    }

    fun setTitle(value: CharSequence?, rightDrawable: Drawable?) {
        if (value != null && titleTextView[0] == null) {
            createTitleTextView(0)
        }
        if (titleTextView[0] != null) {
            titleTextView[0]!!.visibility =
                if (value != null && !isSearchFieldVisible) VISIBLE else INVISIBLE
            titleTextView[0]!!.setText(value.also { lastTitle = it })
            titleTextView[0]!!.setRightDrawable(rightDrawable.also { lastRightDrawable = it })
            titleTextView[0]!!.setRightDrawableOnClick(rightDrawableOnClickListener)
        }
        fromBottom = false
    }

    fun setRightDrawableOnClick(onClickListener: OnClickListener?) {
        rightDrawableOnClickListener = onClickListener
    }

    private fun setTitleColor(color: Int) {
        if (titleTextView[0] == null) {
            createTitleTextView(0)
        }
        titleColorToSet = color
        titleTextView[0]?.setContentColor(color)
        titleTextView[1]?.setContentColor(color)
    }

    fun setSubtitleColor(color: Int) {
        if (subtitleTextView == null) {
            createSubtitleTextView()
        }
        subtitleTextView?.setContentColor(color)
    }

    fun setTitleScrollNonFitText(b: Boolean) {
    }

    fun setPopupItemsColor(color: Int, icon: Boolean, forActionMode: Boolean) {
    }

    fun setPopupItemsSelectorColor(color: Int, forActionMode: Boolean) {
    }

    fun setPopupBackgroundColor(color: Int, forActionMode: Boolean) {
    }

    fun getSubtitleTextView(): SimpleTextView? {
        return subtitleTextView
    }

    fun getTitleTextView(): SimpleTextView? {
        return titleTextView[0]
    }

    val titleTextView2: SimpleTextView?
        get() = titleTextView[1]

    var title: String?
        get() {
            return if (titleTextView[0] == null) {
                null
            } else titleTextView[0]!!.getText().toString()
        }
        set(value) {
            setTitle(value, null)
        }

    fun getSubtitle(): String? {
        return if (subtitleTextView == null || subtitle == null) {
            null
        } else subtitle.toString()
    }

    fun createMenu(): ActionBarMenu {
        if (menu != null) {
            return menu!!
        }
        menu = ActionBarMenu(context, this)
        addView(
            menu,
            0,
            LayoutHelper.createFrameWithSize(
                LayoutHelper.WRAP_CONTENT,
                getCurrentActionBarHeight(context),
                Gravity.RIGHT or Gravity.BOTTOM
            )
        )
        return menu!!
    }

    fun useWeChatMenu() {
        useWechatMenuStyle = true

        val floatBar = builder(this)
        floatBar.visibility = View.VISIBLE

        setBackMenuVisible(false)
    }

    fun setBackMenuVisible(visible: Boolean) {
        backButtonImageView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun onDrawCrossfadeBackground(canvas: Canvas?) {
        if (blurredBackground && actionBarColor != Color.TRANSPARENT) {
            rectTmp[0, 0, measuredWidth] = measuredHeight
            blurScrimPaint.setColor(actionBarColor)
            contentView!!.drawBlurRect(canvas!!, y, rectTmp, blurScrimPaint, true)
        } else {
            val drawable = background
            if (drawable != null) {
                drawable.setBounds(0, 0, width, height)
                drawable.draw(canvas!!)
            }
        }
    }

    fun onDrawCrossfadeContent(
        canvas: Canvas,
        front: Boolean,
        hideBackDrawable: Boolean,
        progress: Float
    ) {
        for (i in 0 until childCount) {
            val ch = getChildAt(i)
            if ((!hideBackDrawable || ch !== backButtonImageView) && ch.visibility == VISIBLE && ch is View) {
                canvas.save()
                canvas.translate(ch.x, ch.y)
                ch.draw(canvas)
                canvas.restore()
            }
        }
        canvas.save()
        canvas.translate(
            if (front) width * progress * 0.5f else -width * 0.4f * (1f - progress),
            0f
        )
        for (i in 0 until childCount) {
            val ch = getChildAt(i)
            if ((!hideBackDrawable || ch !== backButtonImageView) && ch.visibility == VISIBLE && ch !is View) {
                canvas.save()
                canvas.translate(ch.x, ch.y)
                ch.draw(canvas)
                canvas.restore()
            }
        }
        canvas.restore()
    }

    override fun setBackgroundColor(color: Int) {
        super.setBackgroundColor(color.also { actionBarColor = it })
        if (backButtonImageView != null) {
        }
    }

    fun getBackgroundColor(): Int {
        return actionBarColor
    }

    var backgroundUpdateListener: Runnable? = null
    var searchVisibleAnimator: AnimatorSet? = null
    fun listenToBackgroundUpdate(invalidate: Runnable?) {
        backgroundUpdateListener = invalidate
    }

    protected fun onSearchChangedIgnoreTitles(): Boolean {
        return false
    }

    fun onSearchFieldVisibilityChanged(visible: Boolean) {
        isSearchFieldVisible = visible
        if (searchVisibleAnimator != null) {
            searchVisibleAnimator!!.cancel()
        }
        searchVisibleAnimator = AnimatorSet()
        val viewsToHide = ArrayList<View?>()
        val ignoreTitles = onSearchChangedIgnoreTitles()
        if (!ignoreTitles) {
            if (titleTextView[0] != null) {
                viewsToHide.add(titleTextView[0])
            }
            if (subtitleTextView != null && !TextUtils.isEmpty(subtitle)) {
                viewsToHide.add(subtitleTextView)
                subtitleTextView?.visibility = if (visible) INVISIBLE else VISIBLE
            }
        }
        val alphaUpdate = ValueAnimator.ofFloat(searchFieldVisibleAlpha, if (visible) 1f else 0f)
        alphaUpdate.addUpdateListener { anm: ValueAnimator ->
            searchFieldVisibleAlpha = anm.getAnimatedValue() as Float
            if (backgroundUpdateListener != null) {
                backgroundUpdateListener!!.run()
            }
        }
        searchVisibleAnimator!!.playTogether(alphaUpdate)
        for (i in viewsToHide.indices) {
            val view = viewsToHide[i]
            if (!visible) {
                view!!.visibility = VISIBLE
                view.setAlpha(0f)
                view.scaleX = 0.95f
                view.scaleY = 0.95f
            }
            searchVisibleAnimator!!.playTogether(
                ObjectAnimator.ofFloat(
                    view,
                    ALPHA,
                    if (visible) 0f else 1f
                )
            )
            searchVisibleAnimator!!.playTogether(
                ObjectAnimator.ofFloat(
                    view,
                    SCALE_Y,
                    if (visible) 0.95f else 1f
                )
            )
            searchVisibleAnimator!!.playTogether(
                ObjectAnimator.ofFloat(
                    view,
                    SCALE_X,
                    if (visible) 0.95f else 1f
                )
            )
        }
        if (avatarSearchImageView != null) {
            avatarSearchImageView?.setVisibility(VISIBLE)
            searchVisibleAnimator?.playTogether(
                ObjectAnimator.ofFloat<View>(
                    avatarSearchImageView,
                    ALPHA,
                    if (visible) 1f else 0f
                )
            )
        }
        centerScale = true
        requestLayout()
        searchVisibleAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                for (i in viewsToHide.indices) {
                    val view = viewsToHide[i]
                    if (visible) {
                        view!!.visibility = INVISIBLE
                        view.setAlpha(0f)
                    } else {
                        view!!.setAlpha(1f)
                    }
                }
                if (visible && !ignoreTitles) {
                    titleTextView[0]?.setVisibility(GONE)
                    titleTextView[1]?.setVisibility(GONE)
                }
                if (avatarSearchImageView != null) {
                    if (!visible) {
                        avatarSearchImageView?.setVisibility(GONE)
                    }
                }
            }
        })
        searchVisibleAnimator!!.setDuration(150).start()
        if (backButtonImageView != null) {
        }
    }

    fun setInterceptTouches(value: Boolean) {
        interceptTouches = value
    }

    fun setInterceptTouchEventListener(listener: OnTouchListener?) {
        interceptTouchEventListener = listener
    }

    fun setExtraHeight(value: Int) {
        extraHeight = value
    }

    @JvmOverloads
    fun closeSearchField(closeKeyboard: Boolean = true) {
        if (!isSearchFieldVisible) {
            return
        }
    }

    fun openSearchField(text: String?, animated: Boolean) {
    }

    fun openSearchField(animated: Boolean) {
    }

    fun setSearchFilter() {
    }

    fun clearSearchFilters() {
    }

    fun setSearchFieldText(text: String?) {
    }

    fun onSearchPressed() {
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (backButtonImageView != null) {
            backButtonImageView!!.setEnabled(enabled)
        }
    }

    override fun requestLayout() {
        if (ignoreLayoutRequest) {
            return
        }
        super.requestLayout()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val actionBarHeight: Int = getCurrentActionBarHeight(context)
        val actionBarHeightSpec = MeasureSpec.makeMeasureSpec(actionBarHeight, MeasureSpec.EXACTLY)

        setMeasuredDimension(width, actionBarHeight + (if (occupyStatusBarVisible) AndroidUtils.statusBarHeight else 0) + extraHeight)

        val textLeft: Int = if (backButtonImageView != null && backButtonImageView!!.visibility != GONE) {
            backButtonImageView!!.measure(
                MeasureSpec.makeMeasureSpec(AndroidUtils.dp(54), MeasureSpec.EXACTLY),
                actionBarHeightSpec
            )
            AndroidUtils.dp(if (AndroidUtils.isTablet(context)) 80 else 72)
        } else {
            AndroidUtils.dp(if (AndroidUtils.isTablet(context)) 26 else 18)
        }

        for (i in 0..1) {
            if ((titleTextView[0] != null && titleTextView[0]!!.visibility != GONE) || (subtitleTextView != null && subtitleTextView!!.visibility != GONE) ) {
                val availableWidth: Int = width - AndroidUtils.dp(16) - textLeft - titleRightMargin
                if ((fromBottom && i == 0 || !fromBottom && i == 1) && overlayTitleAnimation && titleAnimationRunning) {
                    titleTextView[i]!!.setTextSize(if (!AndroidUtils.isTablet(context) && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 18 else 20)
                } else {
                    if (titleTextView[0] != null && titleTextView[0]!!.visibility != GONE && subtitleTextView != null && subtitleTextView!!.visibility != GONE) {
                        titleTextView[i]?.setTextSize(if (AndroidUtils.isTablet(context)) 20 else 18)
                        subtitleTextView?.setTextSize(if (AndroidUtils.isTablet(context)) 16 else 14)
                        additionalSubtitleTextView?.setTextSize(if (AndroidUtils.isTablet(context)) 16 else 14)
                    } else {
                        if (titleTextView[i] != null && titleTextView[i]!!.visibility != GONE) {
                            titleTextView[i]!!.setTextSize(if (!AndroidUtils.isTablet(context) && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 18 else 20)
                        }
                        if (subtitleTextView != null && subtitleTextView!!.visibility != GONE) {
                            subtitleTextView?.setTextSize(if (!AndroidUtils.isTablet(context) && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 14 else 16)
                        }
                        if (additionalSubtitleTextView != null) {
                            additionalSubtitleTextView?.setTextSize(if (!AndroidUtils.isTablet(context) && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 14 else 16)
                        }
                    }
                }
                if (titleTextView[i] != null && titleTextView[i]!!.visibility != GONE) {
                    titleTextView[i]?.measure(
                        MeasureSpec.makeMeasureSpec(
                            availableWidth,
                            MeasureSpec.AT_MOST
                        ),
                        MeasureSpec.makeMeasureSpec(
                            AndroidUtils.dp(24) + titleTextView[i]!!.paddingTop + titleTextView[i]!!.paddingBottom,
                            MeasureSpec.AT_MOST
                        )
                    )
                    if (centerScale) {
                        val text: CharSequence = titleTextView[i]!!.getText()
                        titleTextView[i]!!.pivotX =
                            titleTextView[i]!!.paint.measureText(text, 0, text.length) / 2f
                        titleTextView[i]!!.pivotY = (AndroidUtils.dp(24) shr 1).toFloat()
                    } else {
                        titleTextView[i]!!.pivotX = 0f
                        titleTextView[i]!!.pivotY = 0f
                    }
                }
                if (subtitleTextView != null && subtitleTextView!!.visibility != GONE) {
                    subtitleTextView?.measure(
                        MeasureSpec.makeMeasureSpec(
                            availableWidth,
                            MeasureSpec.AT_MOST
                        ), MeasureSpec.makeMeasureSpec(AndroidUtils.dp(20), MeasureSpec.AT_MOST)
                    )
                }
                if (additionalSubtitleTextView != null && additionalSubtitleTextView!!.visibility != GONE) {
                    additionalSubtitleTextView?.measure(
                        MeasureSpec.makeMeasureSpec(
                            availableWidth,
                            MeasureSpec.AT_MOST
                        ), MeasureSpec.makeMeasureSpec(AndroidUtils.dp(20), MeasureSpec.AT_MOST)
                    )
                }
            }
        }
        if (avatarSearchImageView != null) {
            avatarSearchImageView?.measure(
                MeasureSpec.makeMeasureSpec(AndroidUtils.dp(42), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(AndroidUtils.dp(42), MeasureSpec.EXACTLY)
            )
        }

        val childCount = childCount
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE || child === titleTextView[0] || child === titleTextView[1] || child === subtitleTextView || child === backButtonImageView || child === additionalSubtitleTextView || child === avatarSearchImageView) {
                continue
            }
            measureChildWithMargins(child, widthMeasureSpec, 0, MeasureSpec.makeMeasureSpec(actionBarHeight, MeasureSpec.EXACTLY), 0)
        }
    }

    fun setMenuOffsetSuppressed(menuOffsetSuppressed: Boolean) {
        isMenuOffsetSuppressed = menuOffsetSuppressed
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val additionalTop = if (occupyStatusBarVisible) AndroidUtils.statusBarHeight else 0
        val textLeft: Int = if (backButtonImageView != null && backButtonImageView!!.visibility != GONE) {
            backButtonImageView!!.layout(
                0,
                additionalTop,
                backButtonImageView!!.measuredWidth,
                additionalTop + backButtonImageView!!.measuredHeight
            )
            AndroidUtils.dp(if (AndroidUtils.isTablet(context)) 80 else 72)
        } else {
            AndroidUtils.dp(if (AndroidUtils.isTablet(context)) 26 else 18)
        }

        for (i in 0..1) {
            if (titleTextView[i] != null && titleTextView[i]!!.visibility != GONE) {
                val textTop: Int = if ((fromBottom && i == 0 || !fromBottom && i == 1) && overlayTitleAnimation && titleAnimationRunning) {
                        (getCurrentActionBarHeight(context) - titleTextView[i]!!.height) / 2
                    } else {
                        if (subtitleTextView != null && subtitleTextView!!.visibility != GONE) {
                            (getCurrentActionBarHeight(context) / 2 - titleTextView[i]!!.height) / 2 + AndroidUtils.dp(
                                if (!AndroidUtils.isTablet(context) && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 3
                            )
                        } else {
                            (getCurrentActionBarHeight(context) - titleTextView[i]!!.height) / 2
                        }
                    }
                titleTextView[i]?.layout(
                    textLeft,
                    additionalTop + textTop - titleTextView[i]!!.paddingTop,
                    textLeft + titleTextView[i]!!.measuredWidth.coerceAtMost(AndroidUtils.getScreenWidth(context)- textLeft - AndroidUtils.dp(130)),
                    additionalTop + textTop + titleTextView[i]!!.height- titleTextView[i]!!.paddingTop + titleTextView[i]!!.paddingBottom
                )
            }
        }

        if (subtitleTextView != null && subtitleTextView?.visibility !== GONE) {
            val textTop: Int =
                getCurrentActionBarHeight(context) / 2 + (getCurrentActionBarHeight(context) / 2 - subtitleTextView!!.textHeight) / 2 - AndroidUtils.dp(
                    if (!AndroidUtils.isTablet(context) && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 1
                )
            subtitleTextView!!.layout(
                textLeft,
                additionalTop + textTop,
                textLeft + subtitleTextView!!.measuredWidth.coerceAtMost(AndroidUtils.getScreenWidth(context)- textLeft - AndroidUtils.dp(130)),
                additionalTop + textTop + subtitleTextView!!.textHeight
            )
        }
    }

    fun onResume() {
        resumed = true
        updateAttachState()
    }

    protected fun onPause() {
        resumed = false
        updateAttachState()
    }

    fun setAllowOverlayTitle(value: Boolean) {
        allowOverlayTitle = value
    }

    fun setTitleActionRunnable(action: Runnable?) {
        titleActionRunnable = action
        lastRunnable = titleActionRunnable
    }

    var overlayTitleAnimationInProgress = false

    init {
        setOnClickListener { v: View? ->
            if (isSearchFieldVisible) {
                return@setOnClickListener
            }
            if (titleActionRunnable != null) {
                titleActionRunnable!!.run()
            }
        }
    }

    fun setTitleOverlayText(title: String?, titleId: Int, action: Runnable?) {
        if (!allowOverlayTitle || parentFragment?.parentFragment == null) {
            return
        }
        overlayTitleToSet[0] = title
        overlayTitleToSet[1] = titleId
        overlayTitleToSet[2] = action
        if (overlayTitleAnimationInProgress) {
            return
        }
        if (lastOverlayTitle == null && title == null || lastOverlayTitle != null && lastOverlayTitle == title) {
            return
        }
        lastOverlayTitle = title
        var textToSet =  lastTitle
        val rightDrawableToSet = if (title != null) null else lastRightDrawable
        var ellipsize = false
        if (title != null) {
            val index = TextUtils.indexOf(textToSet, "...")
            if (index >= 0) {
                val spannableString = SpannableString.valueOf(textToSet)
                ellipsizeSpanAnimator.wrap(spannableString, index)
                textToSet = spannableString
                ellipsize = true
            }
        }
        titleOverlayShown = title != null
        if (textToSet != null && titleTextView[0] == null || measuredWidth == 0 || titleTextView[0] != null && titleTextView[0]!!.visibility !== VISIBLE) {
            createTitleTextView(0)
            if (supportsHolidayImage) {
                titleTextView[0]?.invalidate()
                invalidate()
            }
            titleTextView[0]?.setText(textToSet)
        } else if (titleTextView[0] != null) {
            titleTextView[0]!!.animate().cancel()
            if (titleTextView[1] != null) {
                titleTextView[1]!!.animate().cancel()
            }
            if (titleTextView[1] == null) {
                createTitleTextView(1)
            }
            titleTextView[1]!!.setText(textToSet)
            overlayTitleAnimationInProgress = true
            val tmp: SimpleTextView? = titleTextView[1]
            titleTextView[1] = titleTextView[0]
            titleTextView[0] = tmp
            titleTextView[0]?.setAlpha(0f)
            titleTextView[0]?.translationY = -AndroidUtils.dp(20).toFloat()

            titleTextView[0]?.animate()
                ?.alpha(1f)
                ?.translationY(0f)
                ?.setDuration(220)?.start()

            val animator =  titleTextView[1]?.animate()?.alpha(0f)?.apply {
                if (subtitleTextView == null) {
                    translationY(AndroidUtils.dp(20).toFloat())
                } else {
                    scaleY(0.7f).scaleX(0.7f)
                }
            }

            requestLayout()
            centerScale = true

            animator?.setDuration(220)?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (titleTextView[1] != null && titleTextView[1]!!.parent != null) {
                        val viewGroup = titleTextView[1]!!.parent as ViewGroup
                        viewGroup.removeView(titleTextView[1])
                    }
                    ellipsizeSpanAnimator.removeView(titleTextView[1]!!)
                    titleTextView[1] = null
                    overlayTitleAnimationInProgress = false
                    setTitleOverlayText(
                        overlayTitleToSet[0] as String?,
                        overlayTitleToSet[1] as Int,
                        overlayTitleToSet[2] as Runnable?
                    )
                }
            })?.start()
        }
        titleActionRunnable = action ?: lastRunnable
    }

    fun setOccupyStatusBar(value: Boolean) {
        occupyStatusBarVisible = value
        actionMode?.setPadding(0, if (occupyStatusBarVisible) AndroidUtils.statusBarHeight else 0, 0, 0)
    }

    fun getOccupyStatusBar(): Boolean {
        return occupyStatusBarVisible
    }

    fun setItemsBackgroundColor(color: Int, isActionMode: Boolean) {
        itemsBackgroundColor = color
        if (backButtonImageView != null) {
            backButtonImageView!!.setBackgroundDrawable(
                ThemeUtils.createSelectorDrawable(
                    itemsBackgroundColor
                )
            )
        }
        if (menu != null) {
            menu!!.updateItemsBackgroundColor()
        }
    }

    fun setItemsColor(color: Int, isActionMode: Boolean) {
        itemsColor = color
        if (backButtonImageView != null) {
            if (itemsColor != 0) {
                val drawable: Drawable = backButtonImageView!!.getDrawable()
                if (drawable is BackDrawable) {
                    drawable.setColor(color)
                } else if (drawable is BitmapDrawable) {
                    backButtonImageView!!.colorFilter = PorterDuffColorFilter(
                        color,
                        PorterDuff.Mode.SRC_IN
                    )
                }
            }
        }
        if (menu != null) {
            menu!!.updateItemsColor()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (forceSkipTouches) {
            false
        } else super.onTouchEvent(event) || interceptTouches
    }

    fun setTitleAnimated(title: CharSequence?, fromBottom: Boolean, duration: Long) {
        setTitleAnimated(title, fromBottom, duration, null)
    }

    fun setTitleAnimated(
        title: CharSequence?,
        fromBottom: Boolean,
        duration: Long,
        interpolator: Interpolator?
    ) {
        if (titleTextView[0] == null || title == null) {
            this.title = title.toString()
            return
        }
        val crossfade = overlayTitleAnimation && !TextUtils.isEmpty(subtitle)
        if (crossfade) {
            if (subtitleTextView!!.visibility != VISIBLE) {
                subtitleTextView?.visibility = VISIBLE
                subtitleTextView?.setAlpha(0f)
            }
            subtitleTextView?.animate()?.alpha(if (fromBottom) 0f else 1f)?.setDuration(220)?.start()
        }
        if (titleTextView[1] != null) {
            if (titleTextView[1]?.parent != null) {
                val viewGroup = titleTextView[1]!!.parent as ViewGroup
                viewGroup.removeView(titleTextView[1])
            }
            titleTextView[1] = null
        }
        titleTextView[1] = titleTextView[0]
        titleTextView[0] = null
        this.title = title.toString()
        this.fromBottom = fromBottom
        titleTextView[0]?.setAlpha(0f)
        if (!crossfade) {
            titleTextView[0]!!.translationY =
                if (fromBottom) AndroidUtils.dp(20).toFloat() else -AndroidUtils.dp(20).toFloat()
        }
        val a1: ViewPropertyAnimator = titleTextView[0]!!.animate().alpha(1f).translationY(0f).setDuration(duration)
        if (interpolator != null) {
            a1.setInterpolator(interpolator)
        }
        a1.start()
        titleAnimationRunning = true
        val a: ViewPropertyAnimator = titleTextView[1]!!.animate().alpha(0f)
        if (!crossfade) {
            a.translationY(if (fromBottom) - AndroidUtils.dp(20).toFloat() else AndroidUtils.dp(20).toFloat())
        }
        if (interpolator != null) {
            a.setInterpolator(interpolator)
        }
        a.setDuration(duration).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (titleTextView[1] != null && titleTextView[1]!!.parent != null) {
                    val viewGroup = titleTextView[1]!!.parent as ViewGroup
                    viewGroup.removeView(titleTextView[1])
                }
                titleTextView[1] = null
                titleAnimationRunning = false
                if (crossfade && fromBottom) {
                    subtitleTextView?.visibility = GONE
                }
                requestLayout()
            }
        }).start()
        requestLayout()
    }

    override fun hasOverlappingRendering(): Boolean {
        return false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        updateAttachState()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attached = false
        updateAttachState()
    }

    private fun updateAttachState() {
        val attachState = attached && resumed
        if (this.attachState != attachState) {
            this.attachState = attachState
            if (attachState) {
                ellipsizeSpanAnimator.onAttachedToWindow()
            } else {
                ellipsizeSpanAnimator.onDetachedFromWindow()
            }
        }
    }

    fun setDrawBlurBackground(contentView: SizeNotifierFrameLayout) {
        blurredBackground = true
        this.contentView = contentView
        contentView.blurBehindViews.add(this)
        background = null
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (blurredBackground && actionBarColor != Color.TRANSPARENT) {
            rectTmp[0, 0, measuredWidth] = measuredHeight
            blurScrimPaint.setColor(actionBarColor)
            contentView!!.drawBlurRect(canvas, y, rectTmp, blurScrimPaint, true)
        }
        super.dispatchDraw(canvas)
    }

    fun setForceSkipTouches(forceSkipTouches: Boolean) {
        this.forceSkipTouches = forceSkipTouches
    }

    fun setDrawBackButton(b: Boolean) {
        drawBackButton = b
        if (backButtonImageView != null) {
            backButtonImageView!!.invalidate()
        }
    }

    fun setUseContainerForTitles() {
        useContainerForTitles = true
        if (titlesContainer == null) {
            titlesContainer = object : FrameLayout(context) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    setMeasuredDimension(
                        MeasureSpec.getSize(widthMeasureSpec),
                        MeasureSpec.getSize(heightMeasureSpec)
                    )
                }

                override fun onLayout(
                    changed: Boolean,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int
                ) {
                }
            }
            addView(titlesContainer)
        }
    }
    
    companion object {
        fun getCurrentActionBarHeight(context: Context): Int {
            return if (AndroidUtils.isTablet(context)) {
                AndroidUtils.dp(64)
            } else if (AndroidUtils.displaySize.x > AndroidUtils.displaySize.y) {
                AndroidUtils.dp(48)
            } else {
                AndroidUtils.dp(56)
            }
        }
    }
}