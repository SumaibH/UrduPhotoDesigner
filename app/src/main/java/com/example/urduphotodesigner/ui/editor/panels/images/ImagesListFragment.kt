package com.example.urduphotodesigner.ui.editor.panels.images

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.urduphotodesigner.databinding.FragmentBackgroundsListBinding
import com.example.urduphotodesigner.ui.editor.panels.background.backgrounds.ImagesAdapter
import com.example.urduphotodesigner.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImagesListFragment : Fragment() {
    private var _binding: FragmentBackgroundsListBinding? = null
    private val binding get() = _binding!!
    private var categoryName: String = ""

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var imagesAdapter: ImagesAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackgroundsListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            categoryName = it.getString("category")!!
        }
        setEvents()
        initObservers()
    }

    private fun setEvents() {
        imagesAdapter = ImagesAdapter(){ image ->

        }
        binding.backgrounds.adapter = imagesAdapter
    }

    private fun initObservers() {

        lifecycleScope.launch {
            mainViewModel.localImages.collect { images ->
                val imageList =
                    images.filter { it.category.equals(categoryName, ignoreCase = true) }

                imagesAdapter.submitList(imageList)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        fun newInstance(tabName: String): ImagesListFragment {
            val fragment = ImagesListFragment()
            val args = Bundle()
            args.putString("category", tabName)
            fragment.arguments = args
            return fragment
        }
    }
}