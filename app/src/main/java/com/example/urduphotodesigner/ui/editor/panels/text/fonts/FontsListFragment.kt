package com.example.urduphotodesigner.ui.editor.panels.text.fonts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.urduphotodesigner.databinding.FragmentFontsListBinding
import com.example.urduphotodesigner.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FontsListFragment : Fragment() {
    private var _binding: FragmentFontsListBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var englishAdapter: FontsAdapter
    private lateinit var urduAdapter: FontsAdapter

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
        urduAdapter = FontsAdapter(){ font ->

        }
        englishAdapter = FontsAdapter(){ font ->

        }
        binding.urduRV.adapter = urduAdapter
        binding.englishRV.adapter = englishAdapter
    }

    private fun initObservers() {

        lifecycleScope.launch {
            mainViewModel.localFonts.collect { fonts ->
                val urduFonts = fonts.filter { it.font_category.equals("Urdu", ignoreCase = true) }
                val englishFonts = fonts.filter { !it.font_category.equals("Urdu", ignoreCase = true) }

                urduAdapter.submitList(urduFonts)
                englishAdapter.submitList(englishFonts)
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