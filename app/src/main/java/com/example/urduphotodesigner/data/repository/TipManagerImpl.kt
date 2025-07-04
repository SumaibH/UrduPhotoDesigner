package com.example.urduphotodesigner.data.repository

import com.example.urduphotodesigner.common.datastore.PreferenceDataStoreAPI
import com.example.urduphotodesigner.common.datastore.PreferenceDataStoreKeysConstants.EDITOR_SHOWN_KEY
import com.example.urduphotodesigner.common.datastore.PreferenceDataStoreKeysConstants.PALETTE_SHOWN_KEY
import com.example.urduphotodesigner.domain.repo.TipManager
import javax.inject.Inject

class TipManagerImpl @Inject constructor(
    private val prefs: PreferenceDataStoreAPI
) : TipManager {

    override suspend fun shouldShowPaletteTip(): Boolean {
        val shown = prefs.getFirstPreference(
            PALETTE_SHOWN_KEY,
            defaultValue = false
        )
        return if (!shown) {
            prefs.putPreference(PALETTE_SHOWN_KEY, true)
            true
        } else false
    }

    override suspend fun shouldShowEditorTip(): Boolean {
        val shown = prefs.getFirstPreference(
            EDITOR_SHOWN_KEY,
            defaultValue = false
        )
        return if (!shown) {
            prefs.putPreference(EDITOR_SHOWN_KEY, true)
            true
        } else false
    }
}