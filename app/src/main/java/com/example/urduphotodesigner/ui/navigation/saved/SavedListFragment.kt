package com.example.urduphotodesigner.ui.navigation.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.urduphotodesigner.databinding.FragmentSavedListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SavedListFragment : Fragment() {
    private var _binding: FragmentSavedListBinding? = null
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
        _binding = FragmentSavedListBinding.inflate(layoutInflater, container, false)
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