package com.example.urduphotodesigner.ui.editor.panels.text.format

import android.graphics.Paint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.common.canvas.enums.LetterCasing
import com.example.urduphotodesigner.common.canvas.enums.ListStyle
import com.example.urduphotodesigner.common.canvas.enums.ParagraphIndentation
import com.example.urduphotodesigner.common.canvas.enums.TextAlignment
import com.example.urduphotodesigner.common.canvas.enums.TextDecoration
import com.example.urduphotodesigner.databinding.FragmentFormatBinding
import com.example.urduphotodesigner.databinding.FragmentFormattingBinding
import com.example.urduphotodesigner.ui.editor.panels.text.appearance.ColorsListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FormattingFragment : Fragment() {
    private var _binding: FragmentFormattingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CanvasViewModel by activityViewModels()
    private var currentTab: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentTab = arguments?.getString("tab_name")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormattingBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupControlsVisibility()
        setEvents()
    }

    private fun setEvents() {

        val caseCards = listOf(
            binding.defaultCase to LetterCasing.NONE,
            binding.allCaps to LetterCasing.ALL_CAPS,
            binding.lowerCase to LetterCasing.LOWER_CASE,
            binding.titleCase to LetterCasing.TITLE_CASE
        )

        caseCards.forEach { (card, caseType) ->
            card.setOnClickListener {
                val currentCase = viewModel.letterCasing.value ?: LetterCasing.NONE
                // Set letter casing only if the card is different
                if (currentCase != caseType) {
                    viewModel.setLetterCasing(caseType)
                }
                // Update stroke for the selected card
                caseCards.forEach { (otherCard, _) ->
                    otherCard.strokeWidth = if (otherCard == card) 4 else 0
                }
            }
        }

        val decorationCards = listOf(
            binding.bold to TextDecoration.BOLD,
            binding.italic to TextDecoration.ITALIC,
            binding.underLine to TextDecoration.UNDERLINE,
            binding.defaultStyle to TextDecoration.NONE
        )

        viewModel.textDecoration.observe(viewLifecycleOwner) { currentDecorations ->
            // Update strokeWidth for each card based on the current selection in ViewModel
            decorationCards.forEach { (card, decorationType) ->
                card.strokeWidth = if (currentDecorations.contains(decorationType)) 4 else 0
            }
        }

        // Set up click listeners for each card
        decorationCards.forEach { (card, decorationType) ->
            card.setOnClickListener {
                val currentDecorations = viewModel.textDecoration.value ?: emptySet()

                if (decorationType == TextDecoration.NONE) {
                    // If "None" is clicked:
                    if (currentDecorations.isEmpty()) return@setOnClickListener // Do nothing if "None" is already selected

                    // Deselect all decorations and select "None" only
                    viewModel.setTextDecoration(emptySet())

                    // Update strokeWidth: Only "None" should have stroke width applied
                    decorationCards.forEach { (otherCard, _) ->
                        otherCard.strokeWidth = if (otherCard == card) 4 else 0
                    }
                } else {
                    // If any other decoration is clicked:
                    if (currentDecorations.contains(TextDecoration.NONE)) {
                        // Deselect "None" if it's currently selected
                        viewModel.setTextDecoration(currentDecorations - TextDecoration.NONE)
                    }

                    // Toggle the decoration (add or remove it)
                    val updatedDecorations = if (currentDecorations.contains(decorationType)) {
                        currentDecorations - decorationType // Deselect this decoration
                    } else {
                        currentDecorations + decorationType // Select this decoration
                    }

                    // Update the ViewModel with the new set of decorations
                    viewModel.setTextDecoration(updatedDecorations)

                    // Update stroke width for selected decorations only
                    decorationCards.forEach { (otherCard, otherDecorationType) ->
                        otherCard.strokeWidth = if (updatedDecorations.contains(otherDecorationType)) 4 else 0
                    }
                }
            }
        }

        val alignCards = listOf(
            binding.leftAlign to TextAlignment.LEFT,
            binding.centerAlignment to TextAlignment.CENTER,
            binding.rightAlign to TextAlignment.RIGHT,
            binding.justify to TextAlignment.JUSTIFY
        )

        alignCards.forEach { (card, alignType) ->
            card.setOnClickListener {
                val currentAlign = viewModel.textAlignment.value ?: TextAlignment.LEFT
                // Set text alignment only if it's a different selection
                if (currentAlign != alignType) {
                    viewModel.setTextAlignment(alignType)
                }
                // Update stroke for selected alignment
                alignCards.forEach { (otherCard, _) ->
                    otherCard.strokeWidth = if (otherCard == card) 4 else 0
                }
            }
        }

        val paraCards = listOf(
            binding.defaultIndent to ParagraphIndentation.NONE,
            binding.decreaseIndent to ParagraphIndentation.DECREASE_INDENT,
            binding.increaseIndent to ParagraphIndentation.INCREASE_INDENT
        )

        paraCards.forEach { (card, paraType) ->
            card.setOnClickListener {
                val currentPara = viewModel.paragraphIndentation.value ?: ParagraphIndentation.NONE
                // Set paragraph indentation only if it's a different selection
                if (currentPara != paraType) {
                    viewModel.setParagraphIndentation(paraType)
                }
                // Update stroke for selected paragraph indentation
                paraCards.forEach { (otherCard, _) ->
                    otherCard.strokeWidth = if (otherCard == card) 4 else 0
                }
            }
        }

        val listCards = listOf(
            binding.defaultList to ListStyle.NONE,
            binding.numberedList to ListStyle.NUMBERED,
            binding.bulletedList to ListStyle.BULLETED
        )

        listCards.forEach { (card, listType) ->
            card.setOnClickListener {
                val currentList = viewModel.listStyle.value ?: ListStyle.NONE
                // Set list style only if it's a different selection
                if (currentList != listType) {
                    viewModel.setListStyle(listType)
                }
                // Update stroke for selected list style
                listCards.forEach { (otherCard, _) ->
                    otherCard.strokeWidth = if (otherCard == card) 4 else 0
                }
            }
        }

        binding.lineSpace.apply {
            min = 0
            max = 100
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    val mappedLineSpacing = 1.0f + (progress / 100.0f) * 2.0f // Line height range from 1.0 to 3.0
                    binding.lineSpacing.text = "%.2f".format(mappedLineSpacing) // Display with 2 decimal places

                    // Update the ViewModel with the mapped line spacing
                    viewModel.setLineSpacing(mappedLineSpacing)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }

        binding.letterSpace.apply {
            min = 0
            max = 100
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    val mappedLetterSpacing = -0.5f + (progress / 100.0f) * 2.0f // Letter spacing range from -0.5 to 1.5
                    binding.letterSpacing.text = "%.2f".format(mappedLetterSpacing) // Display with 2 decimal places

                    // Update the ViewModel with the mapped letter spacing
                    viewModel.setLetterSpacing(mappedLetterSpacing)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
    }

    private fun initObservers(){
        viewModel.lineSpacing.observe(viewLifecycleOwner) { lineSpace ->
            binding.lineSpace.progress = lineSpace.toInt()
            binding.lineSpacing.text = "$lineSpace"
        }

        viewModel.letterSpacing.observe(viewLifecycleOwner) { letterSpace ->
            binding.letterSpace.progress = letterSpace.toInt()
            binding.letterSpacing.text = "$letterSpace"
        }
    }

    private fun setupControlsVisibility() {
        // only show the relevant controls panel
        when (currentTab?.lowercase()) {
            "spacing" -> {
                // preserve existing width
                binding.lineSpacingCard.visibility = View.VISIBLE
                binding.letterSpacingCard.visibility = View.VISIBLE
            }
            "casing" -> {
                binding.casingCard.visibility = View.VISIBLE
            }
            "decoration" -> {
                binding.decorationCard.visibility = View.VISIBLE
            }
            else -> {
                binding.alignmentKit.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val ARG_TAB_NAME = "tab_name"

        fun newInstance(tabName: String): FormattingFragment {
            val fragment = FormattingFragment()
            val args = Bundle()
            args.putString(ARG_TAB_NAME, tabName)
            fragment.arguments = args
            return fragment
        }
    }
}