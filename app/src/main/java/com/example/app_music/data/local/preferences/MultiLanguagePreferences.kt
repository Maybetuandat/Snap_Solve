package com.example.app_music.data.local.preferences

import android.content.Context


object MultiLanguagePreferences {
    private const val PREF_NAME="language_preference"
    private const val SELECTED_LANGUAGE = "selected_language"
    private const val IS_SELECTED_SYSTEM_LANGUAGE = "is_system_language"

    fun isUsingSystemLanguage(context: Context): Boolean{
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean(IS_SELECTED_SYSTEM_LANGUAGE, true)
    }
    fun getSaveLanguage(context : Context):String{
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getString(SELECTED_LANGUAGE, null).toString()
    }
    fun saveLanguageCode(context: Context, languageCode : String): Boolean{
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (languageCode == "system") {
            preferences.edit()
                .putBoolean(IS_SELECTED_SYSTEM_LANGUAGE, true)
                .apply()
            return true
        } else {
            preferences.edit()
                .putString(SELECTED_LANGUAGE, languageCode)
                .putBoolean(IS_SELECTED_SYSTEM_LANGUAGE, false)
                .apply()
            return true
        }
        return false
    }
}