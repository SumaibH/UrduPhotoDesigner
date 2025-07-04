package com.example.urduphotodesigner.ui.editor.panels.background

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.databinding.FragmentBackgroundsBinding
import com.example.urduphotodesigner.ui.editor.panels.text.TextPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BackgroundsFragment : Fragment() {
    private var _binding: FragmentBackgroundsBinding? = null
    private val binding get() = _binding!!
    private var tabs = emptyList<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackgroundsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvents()
    }

    private fun setEvents() {
        tabs = listOf("Images", "Colors")

        val adapter = BackgroundPagerAdapter(
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

        // Initial style
        updateTabStyles(binding.tabLayout.selectedTabPosition)

        // Apply styles on swipe
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabStyles(position)
            }
        })

    }

    fun updateTabStyles(selectedPosition: Int) {
        for (i in 0 until binding.tabLayout.tabCount) {
            val tabView = binding.tabLayout.getTabAt(i)?.customView
            val root = tabView?.findViewById<ConstraintLayout>(R.id.tabRoot)
            val text = tabView?.findViewById<TextView>(R.id.tabTitle)

            if (i == selectedPosition) {
                root?.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.appColor
                    )
                )
                text?.setTextColor(ContextCompat.getColor(requireContext(), R.color.whiteText))
            } else {
                root?.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.contrast
                    )
                )
                text?.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}