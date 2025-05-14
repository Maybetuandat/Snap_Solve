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
    private const val IS_SELECTED_SYSTEM_LANGUAGE = "is_system_language"




    fun isUsingSystemLanguage(context: Context): Boolean{

        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean(IS_SELECTED_SYSTEM_LANGUAGE, true)
    }
    fun getSystemLanguage(): String {
        Log.e("maybetuandat", Locale.getDefault().language.toString())
        return Locale.getDefault().language
    }

// selected_language : en or vi
    //is_system_language true or false
    // if true return getSystemLanguage()
    //else getLanguage from sharepreferences
    fun getSelectedLanguage(context: Context): String {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val isSystemLanguage = preferences.getBoolean(IS_SELECTED_SYSTEM_LANGUAGE, true)

        if(isSystemLanguage)
        {
            return getSystemLanguage()
        }


        val savedLanguage = preferences.getString(SELECTED_LANGUAGE, null)

        if (savedLanguage != null) {
            Log.d("multilanguage", savedLanguage.toString())
            return savedLanguage
        }
        return getSystemLanguage()
    }


    fun setSelectedLanguage(context: Context, languageCode: String) {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (languageCode == "system") {
            preferences.edit()
                .putBoolean(IS_SELECTED_SYSTEM_LANGUAGE, true)
                .apply()
        } else {
            preferences.edit()
                .putString(SELECTED_LANGUAGE, languageCode)
                .putBoolean(IS_SELECTED_SYSTEM_LANGUAGE, false)
                .apply()
        }
    }


    fun getSupportedLanguages(): List<Language> {
        return listOf(
            Language("system", "System"),
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