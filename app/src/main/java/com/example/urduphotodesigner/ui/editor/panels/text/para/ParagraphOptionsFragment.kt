package com.example.urduphotodesigner.ui.editor.panels.text.para

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.enums.ParagraphAlign
import com.example.urduphotodesigner.databinding.FragmentParagraphOptionsBinding
import com.example.urduphotodesigner.ui.editor.panels.text.colors.ColorsListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ParagraphOptionsFragment : Fragment() {
    private var _binding: FragmentParagraphOptionsBinding? = null
    private val binding get() = _binding!!

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
    }

    private fun setEvents() {
        binding.seekBar.addOnChangeListener { _, value, _ ->
            val fontSize = value.toInt()
            binding.fontTitle.text = getString(R.string.font_size)+": $fontSize"
        }

        // Alignment selection
        val alignCards = listOf(
            binding.left to ParagraphAlign.LEFT_ALIGN,
            binding.center to ParagraphAlign.CENTER_ALIGN,
            binding.right to ParagraphAlign.RIGHT_ALIGN,
            binding.justify to ParagraphAlign.JUSTIFY
        )

        alignCards.forEach { (card, alignType) ->
            card.setOnClickListener {
                ParagraphAlign.entries.forEach {
                    it.isSelected = it == alignType
                }

                // Update stroke styles
                alignCards.forEach { (c, a) ->
                    c.strokeWidth = if (a.isSelected) 4 else 0
                }

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