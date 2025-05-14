package com.example.app_music.presentation.feature.common

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.app_music.domain.utils.MultiLanguage


//trong kotlin thi mac dinh cac class la final  -> de cho the cho phep ke thua can them tu khoa open

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {

        val languageCode = MultiLanguage.getSelectedLanguage(newBase)
        val context = MultiLanguage.applyLanguage(newBase, languageCode)
        super.attachBaseContext(context)
    }
}