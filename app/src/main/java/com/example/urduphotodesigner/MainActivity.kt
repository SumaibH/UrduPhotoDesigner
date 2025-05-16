package com.example.urduphotodesigner

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.example.urduphotodesigner.databinding.ActivityMainBinding
import com.example.urduphotodesigner.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding?= null
    private val binding get() = _binding!!

    private var _navController: NavController? = null
    private val navController get() = _navController!!

    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initObservers()
    }

    private fun initObservers() {
        lifecycleScope.launch {
            mainViewModel.localFonts.collect { fonts ->
                // just a log for test
                Log.d("MainActivity", "Fonts list size: ${fonts.size}")
            }
        }

        lifecycleScope.launch {
            mainViewModel.localImages.collect { images ->
                // just a log for test
                Log.d("MainActivity", "Fonts list size: ${images.size}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _navController = null
        _binding = null
    }
}