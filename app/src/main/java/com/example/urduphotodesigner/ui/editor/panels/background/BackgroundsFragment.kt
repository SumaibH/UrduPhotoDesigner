package com.example.urduphotodesigner.ui.editor.panels.background

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.CanvasViewModel
import com.example.urduphotodesigner.databinding.FragmentBackgroundsBinding
import com.example.urduphotodesigner.ui.editor.panels.text.TextPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

@AndroidEntryPoint
class BackgroundsFragment : Fragment() {
    private var _binding: FragmentBackgroundsBinding? = null
    private val binding get() = _binding!!
    private var tabs = emptyList<String>()
    private val viewModel: CanvasViewModel by activityViewModels()

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handlePickedUri(it) }

        }

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

        binding.addImage.setOnClickListener { pickImage.launch("image/*") }

    }

    private fun handlePickedUri(uri: Uri) {
        // You probably don’t want to block the UI thread—do this in IO
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val compressedBytes = loadAndCompressImage(uri)
                withContext(Dispatchers.Main) {
                    viewModel.setCanvasBackgroundImage(BitmapFactory.decodeByteArray(
                        compressedBytes, 0, compressedBytes.size
                    ))
                }
            } catch (e: Exception) {
                Log.e("PhotoPicker", "Failed compressing image", e)
            }
        }
    }

    @Throws(IOException::class)
    private fun loadAndCompressImage(uri: Uri): ByteArray {
        val MAX_IMAGE_BYTES = 500 * 1024 // 500 KB
        val bmp = getBitmapFromUri(uri)

        // 1) Always do at least one compress into baos
        val baos = ByteArrayOutputStream().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // R+ supports true lossless WebP
                bmp.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, this)
            } else {
                // older devices: high-quality JPEG as a starting point
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, this)
            }
        }

        var data = baos.toByteArray()
        if (data.size <= MAX_IMAGE_BYTES) {
            return data
        }

        for (quality in listOf(90, 80, 70, 60)) {
            baos.reset()
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            data = baos.toByteArray()
            if (data.size <= MAX_IMAGE_BYTES) {
                return data
            }
        }

        return data
    }

    @Throws(IOException::class)
    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val resolver = requireContext().contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(resolver, uri)
        }
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