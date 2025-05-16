package com.example.urduphotodesigner.ui.editor.panels.text.colors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.urduphotodesigner.common.Constants
import com.example.urduphotodesigner.databinding.FragmentColorsListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColorsListFragment : Fragment() {
    private var _binding: FragmentColorsListBinding? = null
    private val binding get() = _binding!!

    private lateinit var colorsAdapter: ColorsAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColorsListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        colorsAdapter = ColorsAdapter(Constants.colorList){ color ->

        }
        binding.colors.apply {
            adapter = colorsAdapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        fun newInstance(): ColorsListFragment {
            return ColorsListFragment()
        }
    }
}