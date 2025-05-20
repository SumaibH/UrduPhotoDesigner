package com.example.urduphotodesigner.ui.editor.panels.text.fonts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.urduphotodesigner.common.DownloadState
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
    private lateinit var englishAdapter: FontsAdapter
    private lateinit var urduAdapter: FontsAdapter
    private var fontEntity: FontEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFontsListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvents()
        initObservers()
    }

    private fun setEvents() {
        urduAdapter = FontsAdapter { font, isDownloaded ->
            fontEntity = font
            if (isDownloaded) {
                mainViewModel.getTypeface(font)?.let {
                    viewModel.setFont(it)
                }
            } else {
                mainViewModel.downloadFont(font)
            }
            // Unselect all in English adapter
            englishAdapter.selectedFontId = null
            urduAdapter.selectedFontId = font.id.toString()
        }

        englishAdapter = FontsAdapter { font, isDownloaded ->
            fontEntity = font
            if (isDownloaded) {
                mainViewModel.getTypeface(font)?.let {
                    viewModel.setFont(it)
                }
            } else {
                mainViewModel.downloadFont(font)
            }
            // Unselect all in Urdu adapter
            urduAdapter.selectedFontId = null
            englishAdapter.selectedFontId = font.id.toString()
        }

        binding.urduRV.adapter = urduAdapter
        binding.englishRV.adapter = englishAdapter
    }

    // In FontsListFragment.kt
    private fun initObservers() {
        lifecycleScope.launch {
            mainViewModel.localFonts.collect { fonts ->
                val urduFonts = fonts.filter { it.font_category.equals("Urdu", ignoreCase = true) }
                val englishFonts = fonts.filter { !it.font_category.equals("Urdu", ignoreCase = true) }

                urduAdapter.submitList(urduFonts)
                englishAdapter.submitList(englishFonts)
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
                        viewModel.setFont(downloadState.typeface)
                        // Update UI to show the font is selected
                        fontEntity?.let { font ->
                            if (font.font_category.equals("Urdu", ignoreCase = true)) {
                                urduAdapter.selectedFontId = font.id.toString()
                            } else {
                                englishAdapter.selectedFontId = font.id.toString()
                            }
                        }
                    }
                    is DownloadState.Success -> {
                        // This case is for non-font downloads or if typeface creation failed
                        fontEntity?.let { font ->
                            if (font.is_downloaded) {
                                mainViewModel.getTypeface(font)?.let {
                                    viewModel.setFont(it)
                                }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        fun newInstance(): FontsListFragment {
            return FontsListFragment()
        }
    }
}