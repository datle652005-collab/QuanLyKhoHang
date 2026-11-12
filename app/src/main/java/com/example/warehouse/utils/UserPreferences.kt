package com.example.warehouse.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_USER_ID = longPreferencesKey("user_id")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_FULL_NAME = stringPreferencesKey("full_name")
        val KEY_ROLE = stringPreferencesKey("role")
        val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[KEY_IS_LOGGED_IN] ?: false
    }

    val currentUserId: Flow<Long?> = context.dataStore.data.map { pref ->
        val id = pref[KEY_USER_ID]
        if (id == null || id <= 0) null else id
    }

    val currentUsername: Flow<String> = context.dataStore.data.map { pref ->
        pref[KEY_USERNAME] ?: ""
    }

    val currentFullName: Flow<String> = context.dataStore.data.map { pref ->
        pref[KEY_FULL_NAME] ?: ""
    }

    val currentUserRole: Flow<String> = context.dataStore.data.map { pref ->
        pref[KEY_ROLE] ?: "STAFF"
    }

    suspend fun saveUserSession(
        userId: Long,
        username: String,
        fullName: String,
        role: String,
        rememberMe: Boolean
    ) {
        context.dataStore.edit { pref ->
            pref[KEY_IS_LOGGED_IN] = true
            pref[KEY_USER_ID] = userId
            pref[KEY_USERNAME] = username
            pref[KEY_FULL_NAME] = fullName
            pref[KEY_ROLE] = role
            pref[KEY_REMEMBER_ME] = rememberMe
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { pref ->
            pref[KEY_IS_LOGGED_IN] = false
            pref[KEY_USER_ID] = -1L
            pref[KEY_USERNAME] = ""
            pref[KEY_FULL_NAME] = ""
            pref[KEY_ROLE] = ""
        }
    }
}
