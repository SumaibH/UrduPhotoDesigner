package com.example.urduphotodesigner.common.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceDataStoreKeysConstants {
    val IS_FIRST_RUN = booleanPreferencesKey("IS_FIRST_RUN")
    val USER_ID_TOKEN = stringPreferencesKey("USER_ID_TOKEN")
    val USER_DISPLAY_NAME = stringPreferencesKey("USER_DISPLAY_NAME")

    val AUTH_TOKEN = stringPreferencesKey("AUTH_TOKEN")
    val LOGGED_IN_USER_ID = stringPreferencesKey("LOGGED_IN_USER_ID")
    val LOGGED_IN_USER_EMAIL = stringPreferencesKey("LOGGED_IN_USER_EMAIL")
    val LOGGED_IN_USER_ROLE = stringPreferencesKey("LOGGED_IN_USER_ROLE")
}