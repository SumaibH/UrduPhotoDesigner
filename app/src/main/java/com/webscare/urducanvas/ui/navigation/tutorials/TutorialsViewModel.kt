package com.webscare.urducanvas.ui.navigation.tutorials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webscare.urducanvas.common.sealed.Response
import com.webscare.urducanvas.data.model.TutorialVideo
import com.webscare.urducanvas.data.repository.TutorialsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TutorialsViewModel @Inject constructor(
    private val repository: TutorialsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<Response<List<TutorialVideo>>>(Response.Loading)
    val state: StateFlow<Response<List<TutorialVideo>>> = _state.asStateFlow()

    init {
        load(forceRefresh = false)
    }

    fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            repository.getTutorials(forceRefresh).collect { _state.value = it }
        }
    }
}
