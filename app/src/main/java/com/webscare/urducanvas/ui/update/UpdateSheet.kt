package com.webscare.urducanvas.ui.update

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.common.utils.Utils.keepBelowStatusBar
import com.webscare.urducanvas.databinding.SheetUpdateAvailableBinding

/**
 * "Update available", as a sheet rather than a dialog.
 *
 * The app has no dialogs anywhere — every decision it asks for comes up from the
 * bottom — and an update landing in a centred card was the one place that broke.
 * The callbacks are handed in by [com.webscare.urducanvas.di.UpdateManager] rather
 * than resolved here, so this knows nothing about Play's update flow.
 *
 * A forced update passes no [onRemindLater]: the second button disappears and the
 * sheet stops being dismissible, which is the only state where the app can insist.
 */
class UpdateSheet : BottomSheetDialogFragment() {

    private var _binding: SheetUpdateAvailableBinding? = null
    private val binding get() = _binding!!

    var onUpdateNow: (() -> Unit)? = null
    var onRemindLater: (() -> Unit)? = null

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SheetUpdateAvailableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Rebuilt by the system after a rotation or process death, with the callbacks
        // gone along with the manager that set them. A sheet whose buttons do nothing
        // is worse than no sheet, and the next update check puts it straight back.
        if (savedInstanceState != null && onUpdateNow == null) {
            dismissAllowingStateLoss()
            return
        }

        val canDismiss = onRemindLater != null
        isCancelable = canDismiss

        binding.subOptionBtn.isVisible = canDismiss
        binding.subOptionBtn.addPressEffect {
            onRemindLater?.invoke()
            dismissAllowingStateLoss()
        }

        binding.continueBtn.addPressEffect {
            onUpdateNow?.invoke()
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.45f)
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
            }
            decorView.setOnSystemUiVisibilityChangeListener { forceImmersiveMode() }
        }

        val sheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        sheet.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottom_sheet_bg)
        sheet.setBackgroundResource(android.R.color.transparent)
        // FLAG_LAYOUT_NO_LIMITS windows are handed zero insets, so this reads the
        // status bar off the host activity instead of off the sheet.
        sheet.keepBelowStatusBar()
        sheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

        BottomSheetBehavior.from(sheet).apply {
            isFitToContents = true
            state = BottomSheetBehavior.STATE_EXPANDED
            // A forced update has nowhere else to go, so it cannot be swiped away.
            isDraggable = onRemindLater != null
            peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
            skipCollapsed = true
        }

        forceImmersiveMode()
    }

    private fun forceImmersiveMode() {
        dialog?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.apply {
                    hide(WindowInsets.Type.navigationBars())
                    systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "update_sheet"

        fun newInstance(
            onUpdateNow: () -> Unit,
            onRemindLater: (() -> Unit)? = null
        ) = UpdateSheet().apply {
            this.onUpdateNow = onUpdateNow
            this.onRemindLater = onRemindLater
        }
    }
}
