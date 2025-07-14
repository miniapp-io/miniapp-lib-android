package io.miniapp.core.openplatform.miniapp.ui.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.utils.ThemeUtils


internal class ActionBarMenu : LinearLayout {
    var drawBlur = true
    var parentActionBar: ActionBar? = null
    var isActionMode = false
    private var ids: ArrayList<Int>? = null

    constructor(context: Context?, layer: ActionBar?) : super(context) {
        orientation = HORIZONTAL
        parentActionBar = layer
    }

    constructor(context: Context?) : super(context)

    fun updateItemsBackgroundColor() {
        val count = childCount
        for (a in 0 until count) {
            val view = getChildAt(a)
            (view as? ActionBarMenuItem)?.setBackgroundDrawable(ThemeUtils.createSelectorDrawable(if (isActionMode) parentActionBar!!.itemsActionModeBackgroundColor else parentActionBar!!.itemsBackgroundColor))
        }
    }

    fun updateItemsColor() {
        val count = childCount
        for (a in 0 until count) {
            val view = getChildAt(a)
            if (view is ActionBarMenuItem) {
                (view as ActionBarMenuItem).setIconColor(if (isActionMode) parentActionBar!!.itemsActionModeColor else parentActionBar!!.itemsColor)
            }
        }
    }

    fun addItem(id: Int, drawable: Drawable?): ActionBarMenuItem {
        return addItem(
            id,
            0,
            null,
            if (isActionMode) parentActionBar!!.itemsActionModeBackgroundColor else parentActionBar!!.itemsBackgroundColor,
            drawable,
            AndroidUtils.dp(48),
            null
        )
    }

    fun addItem(id: Int, icon: Int): ActionBarMenuItem {
        return addItem(
            id,
            icon,
            if (isActionMode) parentActionBar!!.itemsActionModeBackgroundColor else parentActionBar!!.itemsBackgroundColor
        )
    }

    fun addItem(id: Int, text: CharSequence?): ActionBarMenuItem {
        return addItem(
            id,
            0,
            text,
            if (isActionMode) parentActionBar!!.itemsActionModeBackgroundColor else parentActionBar!!.itemsBackgroundColor,
            null,
            0,
            text
        )
    }

    fun addItem(
        id: Int,
        icon: Int,
        backgroundColor: Int
    ): ActionBarMenuItem {
        return addItem(
            id,
            icon,
            null,
            backgroundColor,
            null,
            AndroidUtils.dp(48),
            null
        )
    }

    fun addItemWithWidth(id: Int, icon: Int, width: Int): ActionBarMenuItem {
        return addItem(
            id,
            icon,
            null,
            if (isActionMode) parentActionBar!!.itemsActionModeBackgroundColor else parentActionBar!!.itemsBackgroundColor,
            null,
            width,
            null
        )
    }

    fun addItemWithWidth(
        id: Int,
        drawable: Drawable?,
        width: Int,
        title: CharSequence?
    ): ActionBarMenuItem {
        return addItem(
            id,
            0,
            null,
            if (isActionMode) parentActionBar!!.itemsActionModeBackgroundColor else parentActionBar!!.itemsBackgroundColor,
            drawable,
            width,
            title
        )
    }

    fun addItemWithWidth(id: Int, icon: Int, width: Int, title: CharSequence?): ActionBarMenuItem {
        return addItem(
            id,
            icon,
            null,
            if (isActionMode) parentActionBar!!.itemsActionModeBackgroundColor else parentActionBar!!.itemsBackgroundColor,
            null,
            width,
            title
        )
    }

    fun addItem(
        id: Int,
        icon: Int,
        text: CharSequence?,
        backgroundColor: Int,
        drawable: Drawable?,
        width: Int,
        title: CharSequence?
    ): ActionBarMenuItem {
        if (ids == null) {
            ids = ArrayList()
        }
        ids!!.add(id)
        return addItemAt(
            -1,
            id,
            icon,
            text,
            backgroundColor,
            drawable,
            width,
            title
        )
    }

    protected fun addItemAt(
        index: Int,
        id: Int,
        icon: Int,
        text: CharSequence?,
        backgroundColor: Int,
        drawable: Drawable?,
        width: Int,
        title: CharSequence?
    ): ActionBarMenuItem {
        val menuItem = ActionBarMenuItem(
            context,
            this,
            backgroundColor,
            if (isActionMode) parentActionBar!!.itemsActionModeColor else parentActionBar!!.itemsColor,
            text != null
        )
        menuItem.tag = id
        if (text != null) {
            menuItem.textView?.text = text
            val layoutParams = LayoutParams(
                if (width != 0) width else ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutParams.rightMargin = AndroidUtils.dp(14)
            layoutParams.leftMargin = layoutParams.rightMargin
            addView(menuItem, index, layoutParams)
        } else {
            if (drawable != null) {
                menuItem.iconView?.setImageDrawable(drawable)
            } else if (icon != 0) {
                menuItem.iconView?.setImageResource(icon)
            }
            addView(menuItem, index, LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        menuItem.setOnClickListener { view ->
           onItemClick(view.tag as Int)
        }
        if (title != null) {
            menuItem.setContentDescription(title)
        }
        return menuItem
    }

    fun lazilyAddItem(id: Int, icon: Int): LazyItem {
        return lazilyAddItem(
            id,
            icon,
            null,
            if (isActionMode) parentActionBar!!.itemsActionModeBackgroundColor else parentActionBar!!.itemsBackgroundColor,
            null,
            AndroidUtils.dp(48),
            null
        )
    }

    fun lazilyAddItem(
        id: Int,
        icon: Int,
        text: CharSequence?,
        backgroundColor: Int,
        drawable: Drawable?,
        width: Int,
        title: CharSequence?,
    ): LazyItem {
        if (ids == null) {
            ids = ArrayList()
        }
        ids!!.add(id)
        return LazyItem(
            this,
            id,
            icon,
            text,
            backgroundColor,
            drawable,
            width,
            title
        )
    }

    class LazyItem(
        var parent: ActionBarMenu,
        var id: Int,
        var icon: Int,
        var text: CharSequence?,
        var backgroundColor: Int,
        var drawable: Drawable?,
        var width: Int,
        var title: CharSequence?
    ) {
        var contentDescription: CharSequence? = null
        var alpha = 1f
        var overrideMenuClick: Boolean = false
        var allowCloseAnimation: Boolean = false
        var isSearchField: Boolean = false
        var searchFieldHint: CharSequence? = null
        var visibility = GONE
        var cell: ActionBarMenuItem? = null
//        fun setVisibility(visibility: Int) {
//            if (this.visibility != visibility) {
//                this.visibility = visibility
//                if (visibility == VISIBLE) {
//                    add()
//                }
//                if (cell != null) {
//                    cell?.setVisibility(visibility)
//                }
//            }
//        }

//        fun getVisibility(): Int {
//            return visibility
//        }

        var tag: Any? = null

        val view: ActionBarMenuItem?
            get() = cell

        fun createView(): ActionBarMenuItem? {
            add()
            return cell
        }

//        fun setContentDescription(contentDescription: CharSequence?) {
//            this.contentDescription = contentDescription
//            if (cell != null) {
//                cell?.setContentDescription(contentDescription)
//            }
//        }

//        fun setOverrideMenuClick(value: Boolean) {
//            overrideMenuClick = value
//            if (cell != null) {
//                cell?.setOverrideMenuClick(value)
//            }
//        }

//        fun setAllowCloseAnimation(value: Boolean) {
//            allowCloseAnimation = value
//            if (cell != null) {
//                cell?.setAllowCloseAnimation(allowCloseAnimation)
//            }
//        }

        fun setViewAlpha(alpha: Float) {
            this.alpha = alpha
            if (cell != null) {
                cell?.setAlpha(alpha)
            }
        }

        fun add() {
            if (cell != null) {
                return
            }
            var index = parent.childCount
            if (parent.ids != null) {
                val myIndex = parent.ids!!.indexOf(id)
                for (i in 0 until parent.childCount) {
                    val child = parent.getChildAt(i)
                    val tag = child.tag
                    if (tag is Int) {
                        val thisIndex = parent.ids!!.indexOf(tag)
                        if (thisIndex > myIndex) {
                            index = i
                            break
                        }
                    }
                }
            }
            cell = parent.addItemAt(
                index,
                id,
                icon,
                text,
                backgroundColor,
                drawable,
                width,
                title
            )
            cell?.visibility = visibility
            if (contentDescription != null) {
                cell?.setContentDescription(contentDescription)
            }
            cell?.setAllowCloseAnimation(allowCloseAnimation)
            cell?.setOverrideMenuClick(overrideMenuClick)
            cell?.setAlpha(alpha)
        }
    }

    private fun onItemClick(id: Int) {
        if(true == parentActionBar?.canClickItem) {
            parentActionBar?.actionBarMenuOnItemClick?.onItemClick(id)
        }
    }

    fun clearItems() {
        if (ids != null) {
            ids!!.clear()
        }
        removeAllViews()
    }

    fun onMenuButtonPressed() {
        val count = childCount
        for (a in 0 until count) {
            val view = getChildAt(a)
            if (view is ActionBarMenuItem) {
                val item: ActionBarMenuItem = view as ActionBarMenuItem
                if (item.visibility != VISIBLE) {
                    continue
                }
                if (item.overrideMenuClick) {
                    onItemClick(item.tag as Int)
                    break
                }
            }
        }
    }

    fun getItem(id: Int): ActionBarMenuItem? {
        val v = findViewWithTag<View>(id)
        return if (v is ActionBarMenuItem) {
            v as ActionBarMenuItem
        } else null
    }

    fun setItemVisibility(id: Int, visibility: Int) {
        val item: View? = getItem(id)
        if (item != null) {
            item.visibility = visibility
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        val count = childCount
        for (a in 0 until count) {
            val view = getChildAt(a)
            view.setEnabled(enabled)
        }
    }

    fun getItemsMeasuredWidth(ignoreAlpha: Boolean): Int {
        var w = 0
        val count = childCount
        for (a in 0 until count) {
            val view = getChildAt(a)
            if (!ignoreAlpha && (view.alpha == 0f || view.visibility != VISIBLE)) {
                continue
            }
            if (view is ActionBarMenuItem) {
                w += view.measuredWidth
            }
        }
        return w
    }

    val visibleItemsMeasuredWidth: Int
        get() {
            var w = 0
            var i = 0
            val count = childCount
            while (i < count) {
                val view = getChildAt(i)
                if (view is ActionBarMenuItem && view.visibility != GONE) {
                    w += view.measuredWidth
                }
                i++
            }
            return w
        }

    fun translateXItems(offset: Float) {
        val count = childCount
        for (a in 0 until count) {
            val view = getChildAt(a)
            if (view is ActionBarMenuItem) {
                (view as ActionBarMenuItem).setTransitionOffset(offset)
            }
        }
    }

    private var onLayoutListener: Runnable? = null
    fun setOnLayoutListener(listener: Runnable?) {
        onLayoutListener = listener
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (onLayoutListener != null) {
            onLayoutListener!!.run()
        }
    }
}
