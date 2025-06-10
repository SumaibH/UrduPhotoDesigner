package com.example.urduphotodesigner.ui.editor.panels.text.fonts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.urduphotodesigner.common.utils.DownloadState
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.data.model.FontEntity
import com.example.urduphotodesigner.databinding.FragmentFontsListBinding
import com.example.urduphotodesigner.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FontsListFragment : Fragment() {
    private var _binding: FragmentFontsListBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: CanvasViewModel by activityViewModels()
    private lateinit var fontsAdapter: FontsAdapter
    private var fontEntity: FontEntity? = null

    private var currentCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentCategory = arguments?.getString("font_category")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFontsListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        initObservers()
    }

    private fun setupRecyclerViews() {
        fontsAdapter = FontsAdapter { font, isDownloaded ->
            handleFontSelection(font, isDownloaded)
        }
        binding.englishRV.adapter = fontsAdapter
    }

    private fun handleFontSelection(font: FontEntity, isDownloaded: Boolean) {
        if (isDownloaded) {
            viewModel.setFont(font)
        } else {
            // Initiate download
            fontEntity = font // Keep track of the font being downloaded
            mainViewModel.downloadFont(font)
        }
    }

    // In FontsListFragment.kt
    private fun initObservers() {
        lifecycleScope.launch {
            mainViewModel.localFonts.collect { fonts ->
                val filteredFonts = fonts.filter {
                    it.font_category.equals(currentCategory, ignoreCase = true)
                }
                fontsAdapter.submitList(filteredFonts)
            }
        }

        lifecycleScope.launch {
            mainViewModel.downloadState.collect { downloadState ->
                when (downloadState) {
                    is DownloadState.Progress -> {
                        // Optional: Show progress in UI if needed
                    }
                    is DownloadState.SuccessWithTypeface -> {
                        // Automatically apply the font to canvas
                        viewModel.setFont(downloadState.fontEntity)
                        // Update UI to show the font is selected
                        fontEntity?.let { font ->
                            fontsAdapter.selectedFontId = font.id.toString()
                        }
                        mainViewModel.clearDownloadState()
                    }
                    is DownloadState.Success -> {
                        // This case is for non-font downloads or if typeface creation failed
                        fontEntity?.let { font ->
                            if (font.is_downloaded) {
                                viewModel.setFont(font)
                            }
                        }
                    }
                    is DownloadState.Error -> {
                        view?.let { Snackbar.make(it, "Download failed!", Snackbar.LENGTH_SHORT).show() }
                        fontEntity = null
                    }
                    else -> {}
                }
            }
        }

        viewModel.currentFont.observe(viewLifecycleOwner) { currentTypeface ->
            // Clear previous selections
            fontsAdapter.selectedFontId = null

            // Find the font that matches the current typeface by comparing file paths
            mainViewModel.localFonts.value.forEach { font ->
                if (font.is_downloaded && font.id.toString() == currentTypeface?.id.toString()) { // Compare font.id with currentTypeface.id
                    fontsAdapter.selectedFontId = font.id.toString()
                    return@forEach // Exit the loop once the matching font is found
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val ARG_FONT_CATEGORY = "font_category"

        fun newInstance(fontCategory: String): FontsListFragment {
            val fragment = FontsListFragment()
            val args = Bundle()
            args.putString(ARG_FONT_CATEGORY, fontCategory)
            fragment.arguments = args
            return fragment
        }
    }

}