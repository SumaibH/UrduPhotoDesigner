package com.example.urduphotodesigner.ui.editor.panels.text.appearance.childs.gradient

import androidx.lifecycle.*
import com.example.urduphotodesigner.domain.repo.TipManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GradientViewModel @Inject constructor(
  private val tipManager: TipManager
) : ViewModel() {

  private val _showPaletteTip = MutableLiveData<Unit>()
  val showPaletteTip: LiveData<Unit> = _showPaletteTip

  private val _showEditorTip  = MutableLiveData<Unit>()
  val showEditorTip: LiveData<Unit>  = _showEditorTip

  /** Call once the palette screen is visible */
  fun onPaletteScreenShown() {
    viewModelScope.launch {
      if (tipManager.shouldShowPaletteTip()) {
        _showPaletteTip.postValue(Unit)
      }
    }
  }

  /** Call right after “+” is clicked */
  fun onGradientEditorOpened() {
    viewModelScope.launch {
      if (tipManager.shouldShowEditorTip()) {
        _showEditorTip.postValue(Unit)
      }
    }
  }
}
