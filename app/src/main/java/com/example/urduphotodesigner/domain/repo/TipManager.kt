package com.example.urduphotodesigner.domain.repo

interface TipManager {
  suspend fun shouldShowPaletteTip(): Boolean
  suspend fun shouldShowEditorTip(): Boolean
}