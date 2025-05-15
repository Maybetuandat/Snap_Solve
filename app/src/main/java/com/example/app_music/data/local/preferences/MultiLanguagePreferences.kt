package com.example.app_music.data.local.preferences

import android.content.Context
import android.util.Log

object MultiLanguagePreferences {
    private const val PREF_NAME = "language_preference"
    private const val SELECTED_LANGUAGE = "selected_language"
    private const val IS_SELECTED_SYSTEM_LANGUAGE = "is_system_language"
    private const val TAG = "MultiLanguagePrefs"

    fun isUsingSystemLanguage(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val result = preferences.getBoolean(IS_SELECTED_SYSTEM_LANGUAGE, true)
        Log.d(TAG, "isUsingSystemLanguage: $result")
        return result
    }

    fun getSaveLanguage(context: Context): String {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val language = preferences.getString(SELECTED_LANGUAGE, "en") ?: "en"
        Log.d(TAG, "getSaveLanguage: $language")
        return language
    }

    fun saveLanguageCode(context: Context, languageCode: String): Boolean {
        try {
            Log.d(TAG, "saveLanguageCode called with: $languageCode")
            val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val editor = preferences.edit()

            if (languageCode == "system") {
                Log.d(TAG, "Setting to system language")
                editor.putBoolean(IS_SELECTED_SYSTEM_LANGUAGE, true)
                // Still save a default language code even for system mode
                editor.putString(SELECTED_LANGUAGE, "en")
                val result = editor.commit()
                Log.d(TAG, "Save result: $result")
                return result
            } else {
                Log.d(TAG, "Setting to specific language: $languageCode")
                editor.putString(SELECTED_LANGUAGE, languageCode)
                editor.putBoolean(IS_SELECTED_SYSTEM_LANGUAGE, false)
                val result = editor.commit()
                Log.d(TAG, "Save result: $result")
                return result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving language code: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}