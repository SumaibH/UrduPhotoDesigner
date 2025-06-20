package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.PopupMenu
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.BlendType
import com.example.urduphotodesigner.databinding.FragmentBlendBinding
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.adapters.ColorsAdapter
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.adapters.CustomSpinnerAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlendFragment : Fragment() {
    private var _binding: FragmentBlendBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CanvasViewModel by activityViewModels()
    private lateinit var adapter: CustomSpinnerAdapter
    private val blendingOptions = listOf(
        BlendType.SRC,
        BlendType.DST,
        BlendType.SRC_OVER,
        BlendType.DST_OVER,
        BlendType.SRC_IN,
        BlendType.DST_IN,
        BlendType.SRC_OUT,
        BlendType.DST_OUT,
        BlendType.SRC_ATOP,
        BlendType.DST_ATOP,
        BlendType.XOR,
        BlendType.DARKEN,
        BlendType.LIGHTEN,
        BlendType.ADD,
        BlendType.MULTIPLY,
        BlendType.SCREEN
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlendBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvents()
        initObservers()
    }

    private fun setEvents() {

        adapter = CustomSpinnerAdapter(blendingOptions)

        binding.blendSpinner.setOnClickListener {
            val popupMenu = PopupMenu(requireActivity(), binding.blendSpinner)
            blendingOptions.forEachIndexed { index, blendType ->
                popupMenu.menu.add(0, index, index, blendType.name)
            }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedBlendType = blendingOptions[menuItem.itemId]
                viewModel.setBlendingType(selectedBlendType)
                true
            }
            popupMenu.show()
        }

        binding.opacity.apply {
            min = 1
            max = 255
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        viewModel.setOpacityValue(progress)
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }

        binding.radius.apply {
            min = 0
            max = 20
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        viewModel.setBlurValue(progress.toFloat())
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
    }

    private fun initObservers() {
        viewModel.blendingType.observe(viewLifecycleOwner) { type ->
            binding.blendSpinner.text = type.name
        }

        viewModel.opacity.observe(viewLifecycleOwner) { opacity ->
            binding.opacitySize.text = "${opacity?.toInt() ?: 0}"
            binding.opacity.progress = opacity?.toInt() ?: 0
        }

        viewModel.blurValue.observe(viewLifecycleOwner) { radius ->
            binding.radiusSize.text = "${radius?.toInt() ?: 0}"
            binding.radius.progress = radius?.toInt() ?: 0
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {

        fun newInstance(): BlendFragment {
            val fragment = BlendFragment()
            return fragment
        }
    }
}