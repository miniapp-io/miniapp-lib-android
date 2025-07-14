package io.miniapp.core.openplatform.miniapp.ui.views

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.CallSuper
import androidx.annotation.FloatRange
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.miniapp.core.R
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils

internal abstract class BaseBottomSheetDialogFragment <VB : ViewBinding> : BottomSheetDialogFragment(), LifecycleOwner {

    /* ==========================================================================================
     * View
     * ========================================================================================== */

    private var _binding: VB? = null

    // This property is only valid between onCreateView and onDestroyView.
    protected val views: VB
        get() = _binding!!

    abstract fun getBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /* ==========================================================================================
     * BottomSheetBehavior
     * ========================================================================================== */

    protected var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null

    open val showExpanded = false

    open val expandedEnable = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = getBinding(inflater, container)

        val wrapper = TopRoundCornerFrameLayout(requireContext())

        wrapper.addView(_binding!!.root)

        val viewLine = ImageView(requireContext())
        viewLine.setImageResource(R.drawable.bg_bottom_sheet_line)
        val drawable = viewLine.drawable
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        val params = FrameLayout.LayoutParams(width, height).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = resources.getDimensionPixelSize(R.dimen.ow3_dialog_body_padding_top)
        }
        viewLine.layoutParams = params
        wrapper.addView(viewLine)

        return wrapper
    }

    @CallSuper
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            val dialog = this as? BottomSheetDialog

            dialog?.setOnShowListener { dialogInterface ->
                val bottomSheetDialog = dialogInterface as BottomSheetDialog
                val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
            }

            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            bottomSheetBehavior = dialog?.behavior
            bottomSheetBehavior?.setPeekHeight(AndroidUtils.dp(400), false)
            if (showExpanded) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }

            bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED && expandedEnable) {
                        dismiss()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        // This ensures that invalidate() is called for static screens that don't
        // subscribe to a ViewModel.
        forceExpandState()
    }

    protected fun setPeekHeightAsScreenPercentage(@FloatRange(from = 0.0, to = 1.0) percentage: Float) {
        context?.let {
            val screenHeight = AndroidUtils.getScreenHeight(it)
            bottomSheetBehavior?.setPeekHeight((screenHeight * percentage).toInt(), true)
        }
    }

    protected fun forceExpandState() {
        if (showExpanded) {
            // Force the bottom sheet to be expanded
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
}