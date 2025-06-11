package io.miniapp.core.openplatform.miniapp.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.utils.LayoutHelper


internal class ActionBarMenuItem(
    context: Context,
    menu: ActionBarMenu,
    backgroundColor: Int,
    iconColor: Int,
    text: Boolean
) : FrameLayout(context) {


    private val parentMenu: ActionBarMenu?
    var iconView: ImageView? = null
    var textView: TextView? = null
    private var isSearchField = false
    private var subMenuOpenSide = 0
    private var yOffset = 0
    private var allowCloseAnimation = true
    var overrideMenuClick = false
    private var layoutInScreen = false
    private var ignoreOnTextChange = false
    private var additionalYOffset = 0
    private var additionalXOffset = 0
    private var longClickEnabled = false
    private var forceSmoothKeyboard = false
    private var showSubmenuByMove = true
    private var selectedFilterIndex = -1
    private var transitionOffset = 0f
    private var showSubMenuFrom: View? = null
    private var onClickListener: OnClickListener? = null
    private var fixBackground = false


    override fun setTranslationX(translationX: Float) {
        super.setTranslationX(translationX + transitionOffset)
    }

    fun setLongClickEnabled(value: Boolean) {
        longClickEnabled = value
    }

    fun setFixBackground(fixBackground: Boolean) {
        this.fixBackground = fixBackground
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        if (fixBackground) {
            background.draw(canvas)
        }
        super.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return super.onTouchEvent(event)
    }

    fun setIconColor(color: Int) {
        if (iconView != null) {
            iconView?.setColorFilter(PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY))
        }
        if (textView != null) {
            textView!!.setTextColor(color)
        }
    }

    fun setSubMenuOpenSide(side: Int) {
        subMenuOpenSide = side
    }

    fun setLayoutInScreen(value: Boolean) {
        layoutInScreen = value
    }

    fun setForceSmoothKeyboard(value: Boolean) {
        forceSmoothKeyboard = value
    }

    private fun createPopupLayout() {
    }

    protected fun onDismiss() {}

    fun setText(text: CharSequence?) {
        if (textView == null) {
            return
        }
        textView!!.text = text
    }

    val contentView: View
        get() = if (iconView != null) iconView!! else textView!!

    fun setOverrideMenuClick(value: Boolean): ActionBarMenuItem {
        overrideMenuClick = value
        return this
    }

    fun getOnClickListener(): OnClickListener? {
        return onClickListener
    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l.also { onClickListener = it })
    }

    fun setIgnoreOnTextChange() {
        ignoreOnTextChange = true
    }

    fun isSearchField(): Boolean {
        return isSearchField
    }

    fun setAllowCloseAnimation(value: Boolean): ActionBarMenuItem {
        allowCloseAnimation = value
        return this
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    fun setAdditionalYOffset(value: Int) {
        additionalYOffset = value
    }

    fun setAdditionalXOffset(value: Int) {
        additionalXOffset = value
    }

    private fun updateOrShowPopup(show: Boolean, update: Boolean) {
        var offsetY: Int
        offsetY = if (parentMenu != null) {
            -parentMenu.parentActionBar!!.measuredHeight + parentMenu.top + parentMenu.paddingTop /* - (int) parentMenu.parentActionBar.getTranslationY()*/
        } else {
            val scaleY = scaleY
            -(measuredHeight * scaleY - (if (subMenuOpenSide != 2) translationY else 0f) / scaleY).toInt() + additionalYOffset
        }
        offsetY += yOffset
        val fromView = if (showSubMenuFrom == null) this else showSubMenuFrom!!
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (iconView != null) {
            info.setClassName("android.widget.ImageButton")
        } else if (textView != null) {
            info.setClassName("android.widget.Button")
            if (TextUtils.isEmpty(info.getText())) {
                info.setText(textView!!.getText())
            }
        }
    }

    fun collapseSearchFilters() {
        selectedFilterIndex = -1
    }

    fun setTransitionOffset(offset: Float) {
        transitionOffset = offset
        translationX = 0f
    }

    private var lazyList: ArrayList<Item>? = null
    private var lazyMap: HashMap<Int, Item>? = null

    init {
        parentMenu = menu
        if (text) {
            textView = TextView(context)
            textView!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            textView!!.setGravity(Gravity.CENTER)
            textView!!.setPadding(AndroidUtils.dp(4), 0, AndroidUtils.dp(4), 0)
            textView!!.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)
            if (iconColor != 0) {
                textView!!.setTextColor(iconColor)
            }
            addView(
                textView,
                LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT)
            )
        } else {
            iconView = ImageView(context)
            iconView!!.setScaleType(ImageView.ScaleType.CENTER)
            iconView!!.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)
            addView(
                iconView,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
            )
            if (iconColor != 0) {
                iconView!!.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.MULTIPLY)
            }
        }
    }

    class Item private constructor(var viewType: Int) {
        var id = 0
        var icon = 0
        var iconDrawable: Drawable? = null
        var text: CharSequence? = null
        var dismiss = false
        var needCheck = false
        var viewToSwipeBack: View? = null
        private var view: View? = null
        private var overrideClickListener: OnClickListener? = null
        private var visibility = VISIBLE
        private var rightIconVisibility = VISIBLE
        private var textColor: Int? = null
        private var iconColor: Int? = null
        fun add(parent: ActionBarMenuItem): View? {
            parent.createPopupLayout()
            if (view != null) {
                view!!.visibility = visibility
                if (overrideClickListener != null) {
                    view!!.setOnClickListener(overrideClickListener)
                }
            }
            return view
        }

        fun setVisibility(visibility: Int) {
            this.visibility = visibility
            if (view != null) {
                view!!.visibility = visibility
            }
        }

        fun setOnClickListener(onClickListener: OnClickListener?) {
            overrideClickListener = onClickListener
            if (view != null) {
                view!!.setOnClickListener(overrideClickListener)
            }
        }

//        fun setText(text: CharSequence?) {
//            this.text = text
//        }

//        fun setIcon(icon: Int) {
//            if (icon != this.icon) {
//                this.icon = icon
//            }
//        }

        fun setRightIconVisibility(visibility: Int) {
            if (rightIconVisibility != visibility) {
                rightIconVisibility = visibility
            }
        }

        fun setColors(textColor: Int, iconColor: Int) {
            if (this.textColor == null || this.iconColor == null || this.textColor != textColor || this.iconColor != iconColor) {
                this.textColor = textColor
                this.iconColor = iconColor
            }
        }

        companion object {
            fun asSubItem(
                id: Int,
                icon: Int,
                iconDrawable: Drawable?,
                text: CharSequence?,
                dismiss: Boolean,
                needCheck: Boolean
            ): Item {
                val item = Item(VIEW_TYPE_SUBITEM)
                item.id = id
                item.icon = icon
                item.iconDrawable = iconDrawable
                item.text = text
                item.dismiss = dismiss
                item.needCheck = needCheck
                return item
            }

            fun asColoredGap(): Item {
                return Item(VIEW_TYPE_COLORED_GAP)
            }

            fun asSwipeBackItem(
                icon: Int,
                iconDrawable: Drawable,
                text: String,
                viewToSwipeBack: View
            ): Item {
                val item = Item(VIEW_TYPE_SWIPEBACKITEM)
                item.icon = icon
                item.iconDrawable = iconDrawable
                item.text = text
                item.viewToSwipeBack = viewToSwipeBack
                return item
            }
        }
    }

    fun lazilyAddSwipeBackItem(
        icon: Int,
        iconDrawable: Drawable,
        text: String,
        viewToSwipeBack: View
    ): Item? {
        return putLazyItem(Item.asSwipeBackItem(icon, iconDrawable, text, viewToSwipeBack))
    }

    fun lazilyAddSubItem(id: Int, icon: Int, text: CharSequence?): Item? {
        return lazilyAddSubItem(id, icon, null, text, true, false)
    }

    fun lazilyAddSubItem(id: Int, iconDrawable: Drawable?, text: CharSequence?): Item? {
        return lazilyAddSubItem(id, 0, iconDrawable, text, true, false)
    }

    fun lazilyAddSubItem(
        id: Int,
        icon: Int,
        iconDrawable: Drawable?,
        text: CharSequence?,
        dismiss: Boolean,
        needCheck: Boolean
    ): Item? {
        return putLazyItem(Item.asSubItem(id, icon, iconDrawable, text, dismiss, needCheck))
    }

    fun lazilyAddColoredGap(): Item? {
        return putLazyItem(Item.asColoredGap())
    }

    private fun putLazyItem(item: Item?): Item? {
        if (item == null) {
            return item
        }
        if (lazyList == null) {
            lazyList = ArrayList()
        }
        lazyList!!.add(item)
        if (lazyMap == null) {
            lazyMap = HashMap()
        }
        lazyMap!![item.id] = item
        return item
    }

    private fun findLazyItem(id: Int): Item? {
        return if (lazyMap == null) {
            null
        } else lazyMap!![id]
    }

    private fun layoutLazyItems() {
        if (lazyList == null) {
            return
        }
        for (i in lazyList!!.indices) {
            lazyList!![i].add(this)
        }
        lazyList!!.clear()
    }

    companion object {
        fun addText(
            context: Context,
            text: String?
        ) {
            val textView = TextView(context)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            textView.setPadding(
                AndroidUtils.dp(13),
                AndroidUtils.dp(8),
                AndroidUtils.dp(13),
                AndroidUtils.dp(8)
            )
            textView.text = text
            textView.setTag(1, 1)
            textView.setMaxWidth(AndroidUtils.dp(200))
        }

        fun checkRtl(string: String): Boolean {
            if (TextUtils.isEmpty(string)) {
                return false
            }
            val c = string[0]
            return c.code in 0x590..0x6ff
        }

        // lazy layout to create menu only when needed
        // planned to at some point to override the current logic above
        const val VIEW_TYPE_SUBITEM = 0
        const val VIEW_TYPE_COLORED_GAP = 1
        const val VIEW_TYPE_SWIPEBACKITEM = 2
    }
}
