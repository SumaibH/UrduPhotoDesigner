package com.example.urduphotodesigner.ui.navigation.templates

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.urduphotodesigner.databinding.FragmentTemplatesListBinding
import com.example.urduphotodesigner.ui.navigation.saved.SavedListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TemplatesListFragment : Fragment() {
    private var _binding: FragmentTemplatesListBinding?= null
    private val binding get() = _binding!!
    private var tabName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabName = arguments?.getString("TAB_NAME")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemplatesListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        fun newInstance(tabName: String): SavedListFragment {
            return SavedListFragment().apply {
                arguments = Bundle().apply {
                    putString("TAB_NAME", tabName)
                }
            }
        }
    }
}