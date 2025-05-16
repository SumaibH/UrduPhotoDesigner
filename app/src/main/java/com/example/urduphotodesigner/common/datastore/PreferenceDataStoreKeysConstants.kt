package com.example.urduphotodesigner.common.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceDataStoreKeysConstants {
    val IS_FIRST_RUN = booleanPreferencesKey("IS_FIRST_RUN")
    val CONSENT_GRANTED = booleanPreferencesKey("CONSENT_GRANTED")
    val SERVICE_ENABLED = booleanPreferencesKey("SERVICE_ENABLED")
    val DARK_MODE = booleanPreferencesKey("DARK_MODE")
    val POWER_SAVER_MODE = booleanPreferencesKey("POWER_SAVER_MODE")
    val ALL_APPS_DATA = booleanPreferencesKey("ALL_APPS_DATA")
    val ALL_APPS_WIFI = booleanPreferencesKey("ALL_APPS_WIFI")
    val FOCUS_MODE = booleanPreferencesKey("FOCUS_MODE")
    val SELECTED_LANG_CODE = stringPreferencesKey("SELECTED_LANG_CODE")
    val SELECTED_LANG_POS = intPreferencesKey("SELECTED_LANG_POS")
}