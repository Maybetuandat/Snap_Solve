package com.example.app_music.domain.utils

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import java.util.Locale


object MultiLanguage {
    private const val PREF_NAME="language_preference"
    private const val SELECTED_LANGUAGE = "selected_language"
    private const val LANGUAGE_CHANGED = "language_changed"

    fun getSelectedLanguage(context: Context): String {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        Log.i("multilanguage", preferences.getString(SELECTED_LANGUAGE, "en").toString())
        return preferences.getString(SELECTED_LANGUAGE, "en") ?: "en"  // tra ve gia tri mac dinh la en
    }


    fun setSelectedLanguage(context: Context, languageCode: String) {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentLanguage = getSelectedLanguage(context)

        if (currentLanguage != languageCode) {
            preferences.edit()
                .putString(SELECTED_LANGUAGE, languageCode)  // update lai ngon ngu cho file sharepreference
                .putBoolean(LANGUAGE_CHANGED, true)
                .apply()
        }
    }


    fun getSupportedLanguages(): List<Language> {
        return listOf(
            Language("en", "English"),
            Language("vi", "Tiếng Việt")
        )
    }
    fun applyLanguage(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            configuration.locale = locale
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }

    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}
data class Language(val code: String, val name: String)