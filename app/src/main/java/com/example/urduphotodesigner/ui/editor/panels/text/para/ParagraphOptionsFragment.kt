package com.example.urduphotodesigner.ui.editor.panels.text.para

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.databinding.FragmentParagraphOptionsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ParagraphOptionsFragment : Fragment() {
    private var _binding: FragmentParagraphOptionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CanvasViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParagraphOptionsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvents()
        initObservers()
    }

    private fun initObservers() {
//        viewModel.currentTextSize.observe(viewLifecycleOwner) { textSize ->
//            // Update the SeekBar progress
//            binding.seekBar.value = textSize!!
//            binding.fontTitle.text = "Font Size: ${textSize.toInt()}"
//        }

        viewModel.currentTextAlignment.observe(viewLifecycleOwner) { alignment ->
            val alignCards = listOf(
                binding.left to Paint.Align.LEFT,
                binding.center to Paint.Align.CENTER,
                binding.right to Paint.Align.RIGHT,
            )

            alignCards.forEach { (card, alignType) ->
                card.strokeWidth = if (alignType == alignment) 4 else 0
            }
        }
    }

    private fun setEvents() {

        binding.seekBar.addOnChangeListener { _, value, _ ->
            viewModel.setTextSize(value)
            binding.fontTitle.text = "Font Size: ${value.toInt()}" // Update title dynamically
        }

        val alignCards = listOf(
            binding.left to Paint.Align.LEFT,
            binding.center to Paint.Align.CENTER,
            binding.right to Paint.Align.RIGHT,
        )

        alignCards.forEach { (card, alignType) ->
            card.setOnClickListener {
                // This part is already handled by the observer, but we keep it to trigger the ViewModel update
                viewModel.setTextAlignment(alignType)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        fun newInstance(): ParagraphOptionsFragment {
            return ParagraphOptionsFragment()
        }
    }
}