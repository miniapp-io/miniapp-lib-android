package io.miniapp.core.openplatform.miniapp.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import io.miniapp.core.databinding.FragmentMenusBottomSheetBinding
import io.miniapp.core.openplatform.miniapp.ui.views.BaseBottomSheetDialogFragment
import io.miniapp.core.openplatform.miniapp.ui.views.MenuActionButton

internal class MenusBottomSheet : BaseBottomSheetDialogFragment<FragmentMenusBottomSheetBinding>() {

   private val menus: ArrayList<MenuItem> = arrayListOf()
   private var itemClickListener: OnMenuItemClickListener? = null

    override fun getBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMenusBottomSheetBinding {
        return FragmentMenusBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildMenus()
        views.root.post {
            bottomSheetBehavior?.peekHeight = views.root.measuredHeight
            bottomSheetBehavior?.maxHeight = views.root.measuredHeight
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        itemClickListener = null
        menus.clear()
    }

    private fun buildMenus() {
        val buttonContainer = views.root

        val buttonsPerRow = 4

        var rowLayout: LinearLayout? = null
        var buttonCount = 0

        for (menu in menus) {
            if (buttonCount == buttonsPerRow || rowLayout == null) {
                rowLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.START
                    weightSum = 4.0f
                }
                buttonContainer.addView(rowLayout)
                buttonCount = 0
            }

            val button = MenuActionButton(buttonContainer.context).apply {
                title = menu.title
                toIconRes = menu.iconRes
                id = View.generateViewId()
            }
            button.setOnClickListener {
                itemClickListener?.onItemClick(menu.id)
                dismiss()
            }

            // Set layout parameters
            val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }

            rowLayout.addView(button, params)
            buttonCount++
        }
    }

    companion object {
        fun newInstance(menus: List<MenuItem>, itemClickListener: OnMenuItemClickListener): MenusBottomSheet {
            return MenusBottomSheet().apply {
                this.itemClickListener = itemClickListener
                this.menus.addAll(menus)
            }
        }
    }

    data class MenuItem(val id: String, val iconRes: Int, val title: String)

   fun interface OnMenuItemClickListener {
        fun onItemClick(id: String)
    }

}