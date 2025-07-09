package com.example.urduphotodesigner.ui.editor.panels.objects

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.databinding.FragmentObjectsBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ObjectsFragment : Fragment() {
    private var _binding: FragmentObjectsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ObjectsPagerAdapter
    private var tabs = mutableListOf<String>()
    private var isSearching = false

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

    @SuppressLint("ClickableViewAccessibility")
    private fun setEvents() {
        tabs.addAll(
            listOf(
                "Stickers",
                "Emoticons",
                "Animals",
                "Nature",
                "Food",
                "Sports",
                "Transport",
                "Objects",
                "Alchemy",
                "Shapes",
                "Arrows",
                "Letters",
                "Flags"
            )
        )

        adapter = ObjectsPagerAdapter(
            requireActivity().supportFragmentManager,
            lifecycle,
            tabs
        )

        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val tabView = LayoutInflater.from(context).inflate(R.layout.custom_tab, null)
            tabView.findViewById<TextView>(R.id.tabTitle).text = tabs[position]
            tab.customView = tabView
        }.attach()

        binding.searchBar.imeOptions = EditorInfo.IME_ACTION_SEARCH
        binding.searchBar.setRawInputType(InputType.TYPE_CLASS_TEXT)

        binding.searchBar.setImeActionLabel("🔍", EditorInfo.IME_ACTION_SEARCH)

        binding.searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchBar.text.toString()
                adapter.filter(query)
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext()
            .getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(binding.searchBar.windowToken, 0)
        binding.searchBar.clearFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}