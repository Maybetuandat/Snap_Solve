package com.example.app_music.presentation.feature.common

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.app_music.domain.utils.MultiLanguage

//trong kotlin thi mac dinh cac class la final  -> de cho the cho phep ke thua can them tu khoa open

open class BaseActivity : AppCompatActivity() {
    private val TAG = "BaseActivity"

    override fun attachBaseContext(newBase: Context) {
        try {
            val languageCode = MultiLanguage.getSelectedLanguage(newBase)
            Log.d(TAG, "attachBaseContext - languageCode: $languageCode")

            val isSystem = MultiLanguage.isUsingSystemLanguage(newBase)
            Log.d(TAG, "attachBaseContext - isSystem: $isSystem")

            val context = MultiLanguage.applyLanguage(newBase, languageCode)
            Log.d(TAG, "attachBaseContext - applied language, current locale: ${context.resources.configuration.locales.get(0).language}")

            super.attachBaseContext(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error in attachBaseContext: ${e.message}")
            e.printStackTrace()
            super.attachBaseContext(newBase)
        }
    }
}