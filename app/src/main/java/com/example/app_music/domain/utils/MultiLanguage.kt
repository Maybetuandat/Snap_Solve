package com.example.app_music.domain.utils

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import com.example.app_music.data.local.preferences.MultiLanguagePreferences
import java.util.Locale

object MultiLanguage {
    private const val TAG = "MultiLanguage"

    fun isUsingSystemLanguage(context: Context): Boolean {
        val result = MultiLanguagePreferences.isUsingSystemLanguage(context)
        Log.d(TAG, "isUsingSystemLanguage: $result")
        return result
    }

    fun getSystemLanguage(): String {
        val systemLang = Locale.getDefault().language
        Log.d(TAG, "getSystemLanguage: $systemLang")
        return systemLang
    }

    // selected_language : en or vi
    // is_system_language true or false
    // if true return getSystemLanguage()
    // else getLanguage from sharepreferences
    fun getSelectedLanguage(context: Context): String {
        val isSystemLanguage = isUsingSystemLanguage(context)
        Log.d(TAG, "getSelectedLanguage - isSystemLanguage: $isSystemLanguage")

        if (isSystemLanguage) {
            val sysLang = getSystemLanguage()
            Log.d(TAG, "Using system language: $sysLang")
            return sysLang
        }

        val savedLanguage = MultiLanguagePreferences.getSaveLanguage(context)
        Log.d(TAG, "Using saved language: $savedLanguage")
        return savedLanguage
    }

    fun setSelectedLanguage(context: Context, languageCode: String): Boolean {
        Log.d(TAG, "setSelectedLanguage called with: $languageCode")
        val result = MultiLanguagePreferences.saveLanguageCode(context, languageCode)
        Log.d(TAG, "Language saved: $result")
        return result
    }

    fun getSupportedLanguages(): List<Language> {
        val languages = listOf(
            Language("system", "System"),
            Language("en", "English"),
            Language("vi", "Tiếng Việt")
        )
        Log.d(TAG, "getSupportedLanguages: ${languages.map { it.code }}")
        return languages
    }

    fun applyLanguage(context: Context, languageCode: String): Context {
        Log.d(TAG, "applyLanguage called with: $languageCode")
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        Log.d(TAG, "Default locale set to: ${Locale.getDefault().language}")

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
            Log.d(TAG, "Creating configuration context with locale: $languageCode")
            val newContext = context.createConfigurationContext(configuration)
            Log.d(TAG, "New context created with locale: ${newContext.resources.configuration.locales.get(0).language}")
            return newContext
        } else {
            configuration.locale = locale
            resources.updateConfiguration(configuration, resources.displayMetrics)
            Log.d(TAG, "Updated resources configuration with locale: $languageCode")
            return context
        }
    }

    fun restartApp(context: Context) {
        try {
            Log.d(TAG, "restartApp called")
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            Log.d(TAG, "Launch intent retrieved: $intent")

            val componentName = intent?.component
            Log.d(TAG, "Component name: $componentName")

            val mainIntent = Intent.makeRestartActivityTask(componentName)
            Log.d(TAG, "Created restart intent: $mainIntent")

            context.startActivity(mainIntent)
            Log.d(TAG, "Started restart activity")

            // Ensure the app fully exits
            Runtime.getRuntime().exit(0)
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting app: ${e.message}")
            e.printStackTrace()
            throw e // Rethrow to allow caller to handle
        }
    }
}

data class Language(val code: String, val name: String)