package com.example.urduphotodesigner.viewmodels

import android.graphics.Typeface
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urduphotodesigner.common.Constants
import com.example.urduphotodesigner.common.DownloadState
import com.example.urduphotodesigner.common.Response
import com.example.urduphotodesigner.data.model.FontEntity
import com.example.urduphotodesigner.data.model.ImageEntity
import com.example.urduphotodesigner.domain.repo.FontRepository
import com.example.urduphotodesigner.domain.usecase.FetchAPIFontsUseCase
import com.example.urduphotodesigner.domain.usecase.FetchAPIImagesUseCase
import com.example.urduphotodesigner.domain.usecase.GetFontsUseCase
import com.example.urduphotodesigner.domain.usecase.GetImagesUseCase
import com.example.urduphotodesigner.domain.usecase.InsertFontsUseCase
import com.example.urduphotodesigner.domain.usecase.InsertImagesUseCase
import com.example.urduphotodesigner.domain.usecase.UpdateFontStatusUseCase
import com.example.urduphotodesigner.domain.usecase.UpdateFontsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fetchAPIFontsUseCase: FetchAPIFontsUseCase,
    private val insertFontsUseCase: InsertFontsUseCase,
    private val getFontsUseCase: GetFontsUseCase,
    private val fetchAPIImagesUseCase: FetchAPIImagesUseCase,
    private val insertImagesUseCase: InsertImagesUseCase,
    private val getImagesUseCase: GetImagesUseCase,
    private val fontRepository: FontRepository,
    private val updateFontsUseCase: UpdateFontsUseCase,
    private val updateFontStatusUseCase: UpdateFontStatusUseCase
) : ViewModel() {

    private val _downloadState = MutableStateFlow<DownloadState?>(null)
    val downloadState: StateFlow<DownloadState?> = _downloadState

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

    // In MainViewModel.kt
    fun downloadFont(font: FontEntity) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Progress(0)
            updateFontStatusUseCase.invoke(font.id.toString(), true)

            try {
                val downloadedFile = fontRepository.downloadFont(
                    fontUrl = Constants.BASE_URL_GLIDE+font.file_url,
                    fileName = font.file_name,
                    onProgress = { progress ->
                        _downloadState.value = DownloadState.Progress(progress)
                    }
                )

                updateFontsUseCase.invoke(font.id.toString(),
                    isDownloaded = true,
                    isDownloading = false,
                    filePath = downloadedFile.absolutePath
                )

                // After successful download, get the typeface and update the canvas
                getTypeface(font.copy(
                    is_downloaded = true,
                    file_path = downloadedFile.absolutePath
                ))?.let { typeface ->
                    _downloadState.value = DownloadState.SuccessWithTypeface(downloadedFile, typeface)
                } ?: run {
                    _downloadState.value = DownloadState.Success(downloadedFile)
                }
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error(e.message ?: "Download failed")
            }
        }
    }

    fun getTypeface(font: FontEntity): Typeface? {
        return if (font.is_downloaded && font.file_path != null) {
            try {
                Typeface.createFromFile(font.file_path)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun checkFontDownloaded(font: FontEntity): Boolean {
        return font.is_downloaded && font.file_path?.let { File(it).exists() } ?: false
    }

    fun clearDownloadState() {
        _downloadState.value = null
    }
}
