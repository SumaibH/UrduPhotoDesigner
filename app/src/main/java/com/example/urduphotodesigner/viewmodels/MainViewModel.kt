package com.example.urduphotodesigner.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urduphotodesigner.common.Response
import com.example.urduphotodesigner.data.model.FontEntity
import com.example.urduphotodesigner.data.model.ImageEntity
import com.example.urduphotodesigner.domain.usecase.FetchAPIFontsUseCase
import com.example.urduphotodesigner.domain.usecase.FetchAPIImagesUseCase
import com.example.urduphotodesigner.domain.usecase.GetFontsUseCase
import com.example.urduphotodesigner.domain.usecase.GetImagesUseCase
import com.example.urduphotodesigner.domain.usecase.InsertFontsUseCase
import com.example.urduphotodesigner.domain.usecase.InsertImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fetchAPIFontsUseCase: FetchAPIFontsUseCase,
    private val insertFontsUseCase: InsertFontsUseCase,
    private val getFontsUseCase: GetFontsUseCase,
    private val fetchAPIImagesUseCase: FetchAPIImagesUseCase,
    private val insertImagesUseCase: InsertImagesUseCase,
    private val getImagesUseCase: GetImagesUseCase
) : ViewModel() {

    private val _localFonts = MutableStateFlow<List<FontEntity>>(emptyList())
    val localFonts: StateFlow<List<FontEntity>> = _localFonts.asStateFlow()

    private val _localImages = MutableStateFlow<List<ImageEntity>>(emptyList())
    val localImages: StateFlow<List<ImageEntity>> = _localImages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        Log.d("FontsViewModel", "ViewModel initialized")
        fetchAndStoreFontsFromApi()
        observeLocalFonts()
        fetchAndStoreImagesFromApi()
        observeLocalImages()
    }

    private fun fetchAndStoreFontsFromApi() {
        viewModelScope.launch {
            fetchAPIFontsUseCase().collect { response ->
                when (response) {
                    is Response.Loading -> _isLoading.value = true

                    is Response.Success -> {
                        _isLoading.value = false
                        insertFontsUseCase.invoke(response.data!!)
                    }

                    is Response.Error -> {
                        _isLoading.value = false
                        _error.value = response.message ?: "Unknown error"
                    }

                    else -> {}
                }
            }
        }
    }

    private fun fetchAndStoreImagesFromApi() {
        viewModelScope.launch {
            fetchAPIImagesUseCase().collect { response ->
                when (response) {
                    is Response.Loading -> _isLoading.value = true

                    is Response.Success -> {
                        _isLoading.value = false
                        insertImagesUseCase.invoke(response.data!!)
                    }

                    is Response.Error -> {
                        _isLoading.value = false
                        _error.value = response.message ?: "Unknown error"
                    }

                    else -> {}
                }
            }
        }
    }

    private fun observeLocalImages() {
        viewModelScope.launch {
            getImagesUseCase().collect { images ->
                _localImages.value = images
            }
        }
    }

    private fun observeLocalFonts() {
        viewModelScope.launch {
            getFontsUseCase().collect { fonts ->
                _localFonts.value = fonts
            }
        }
    }
}
