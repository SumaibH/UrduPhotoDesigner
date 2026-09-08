package com.webscare.urducanvas.ui.editor.panels.text.symbols

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.DialogCalligraphyBreakdownBinding

class CalligraphyBreakdownBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogCalligraphyBreakdownBinding? = null
    private val binding get() = _binding!!

    var onBreakWords: (() -> Unit)? = null
    var onBreakCharacters: (() -> Unit)? = null
    var onCollapse: (() -> Unit)? = null
    var isAlreadyExpanded: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogCalligraphyBreakdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCollapseText.visibility = if (isAlreadyExpanded) View.VISIBLE else View.GONE

        binding.btnBreakWords.addPressEffect {
            onBreakWords?.invoke()
            dismiss()
        }

        binding.btnBreakCharacters.addPressEffect {
            onBreakCharacters?.invoke()
            dismiss()
        }

        binding.btnCollapseText.addPressEffect {
            onCollapse?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(isAlreadyExpanded: Boolean): CalligraphyBreakdownBottomSheet {
            return CalligraphyBreakdownBottomSheet().apply {
                this.isAlreadyExpanded = isAlreadyExpanded
            }
        }
    }
}
