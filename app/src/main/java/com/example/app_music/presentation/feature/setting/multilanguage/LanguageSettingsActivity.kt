package com.example.app_music.presentation.feature.setting.multilanguage

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.app_music.R
import com.example.app_music.databinding.ActivityLanguageSettingsBinding
import com.example.app_music.domain.utils.MultiLanguage
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.setting.restartappdialog.RestartAppDialog

class LanguageSettingsActivity : BaseActivity() {
    private lateinit var binding: ActivityLanguageSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {

        val currentLanguage = MultiLanguage.getSelectedLanguage(this)


        val languages = MultiLanguage.getSupportedLanguages()


        binding.radioGroupLanguages.removeAllViews()


        languages.forEach { language ->
            val radioButton = RadioButton(this)
            radioButton.id = View.generateViewId()
            radioButton.text = language.name
            radioButton.tag = language.code
            radioButton.layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            )
            radioButton.setPadding(0, 30, 0, 30)


            if (language.code == currentLanguage) {
                radioButton.isChecked = true
            }

            binding.radioGroupLanguages.addView(radioButton)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.radioGroupLanguages.setOnCheckedChangeListener { group, checkedId ->
            try {
                val radioButton = findViewById<RadioButton>(checkedId)
                val newLanguageCode = radioButton.tag as String
                val currentLanguageCode = MultiLanguage.getSelectedLanguage(this)

                if (newLanguageCode != currentLanguageCode) {

                    MultiLanguage.setSelectedLanguage(this, newLanguageCode)





                    try {
                        val dialog = RestartAppDialog(this)
                        dialog.show()
                    } catch (e: Exception) {

                        Log.e("LanguageSetting", "Error showing dialog: ${e.message}")
                        Toast.makeText(
                            this,
                            "Vui lòng khởi động lại ứng dụng để áp dụng thay đổi ngôn ngữ",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("LanguageSetting", "Error in radio change: ${e.message}")
            }
        }
    }
}