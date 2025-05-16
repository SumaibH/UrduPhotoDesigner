package com.example.urduphotodesigner.ui.editor.panels.objects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.urduphotodesigner.databinding.FragmentObjectsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ObjectsFragment : Fragment() {
    private var _binding: FragmentObjectsBinding? = null
    private val binding get() = _binding!!

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
    }

    private fun setEvents() {

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}