package com.example.urduphotodesigner.ui.navigation.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding?= null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvents()
    }

    private fun setEvents() {
        binding.create.setOnClickListener {
            findNavController().navigate(R.id.createFragment)
        }

        binding.template.setOnClickListener {
            findNavController().navigate(R.id.templatesFragment)
        }

        binding.saved.setOnClickListener {
            findNavController().navigate(R.id.savedFragment)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}