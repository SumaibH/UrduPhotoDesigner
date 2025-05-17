package com.example.urduphotodesigner.ui.editor.panels.objects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.urduphotodesigner.databinding.FragmentObjectsBinding
import com.example.urduphotodesigner.ui.editor.panels.background.backgrounds.ImagesAdapter
import com.example.urduphotodesigner.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ObjectsFragment : Fragment() {
    private var _binding: FragmentObjectsBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var imagesAdapter: ImagesAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentObjectsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    images.filter { it.category.equals("Image", ignoreCase = true) }

                imagesAdapter.submitList(imageList)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}