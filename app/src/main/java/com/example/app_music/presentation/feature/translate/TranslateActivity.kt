package com.example.app_music.presentation.feature.translate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_music.R
import com.example.app_music.databinding.ActivityTranslateBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class TranslateActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityTranslateBinding
    private var textToSpeech: TextToSpeech? = null
    private val translationService = GoogleTranslateService()

    // Language display names
    private var sourceLanguageName = "Vietnamese"
    private var targetLanguageName = "English"

    // Supported languages with their codes
    private val supportedLanguages = mapOf(
        "Vietnamese" to "vi",
        "English" to "en",
        "Chinese" to "zh",
        "Japanese" to "ja",
        "Korean" to "ko",
        "French" to "fr",
        "German" to "de",
        "Spanish" to "es",
        "Thai" to "th",
        "Russian" to "ru",
        "Italian" to "it",
        "Portuguese" to "pt",
        "Arabic" to "ar",
        "Hindi" to "hi"
    )

    private val languageNames = supportedLanguages.keys.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranslateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupTextToSpeech()
        setupListeners()
    }

    private fun setupViews() {
        // Back button
        binding.btnBack.setOnClickListener { finish() }

        // Update language display
        updateLanguageDisplay()

        // Character counter
        updateCharacterCount("")
    }

    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(this, this)
    }

    private fun setupListeners() {
        // Input text change listener
        binding.etInputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                updateCharacterCount(text)

                // Auto-translate after user stops typing
                if (text.isNotEmpty()) {
                    lifecycleScope.launch {
                        delay(1500) // Wait 1.5 seconds after user stops typing
                        if (text == binding.etInputText.text.toString()) {
                            translateText(text)
                        }
                    }
                } else {
                    clearTranslation()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Language selection
        binding.tvSourceLanguage.setOnClickListener {
            showLanguageSelector(true) { language ->
                sourceLanguageName = language
                updateLanguageDisplay()
                retranslateIfNeeded()
            }
        }

        binding.tvTargetLanguage.setOnClickListener {
            showLanguageSelector(false) { language ->
                targetLanguageName = language
                updateLanguageDisplay()
                retranslateIfNeeded()
            }
        }

        // Swap languages
        binding.btnSwapLanguages.setOnClickListener {
            swapLanguages()
        }

        // Copy translation
        binding.btnCopyTranslation.setOnClickListener {
            copyTranslation()
        }

        // Speak translation
        binding.btnSpeakTranslation.setOnClickListener {
            speakTranslation()
        }
    }

    private fun updateCharacterCount(text: String) {
        binding.tvCharCount.text = "${text.length}/1000 characters"

        // Disable input if over limit
        if (text.length > 1000) {
            binding.etInputText.setText(text.substring(0, 1000))
            binding.etInputText.setSelection(1000)
            Toast.makeText(this, "Maximum 1000 characters allowed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLanguageDisplay() {
        binding.tvSourceLanguage.text = sourceLanguageName
        binding.tvTargetLanguage.text = targetLanguageName
    }

    private fun translateText(text: String) {
        if (text.trim().isEmpty()) return

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.tvTranslationResult.text = "Translating..."

        lifecycleScope.launch {
            try {
                val sourceCode = supportedLanguages[sourceLanguageName] ?: "vi"
                val targetCode = supportedLanguages[targetLanguageName] ?: "en"

                val result = translationService.translateText(text, sourceCode, targetCode)

                binding.progressBar.visibility = View.GONE

                if (result != null && result.isNotEmpty()) {
                    binding.tvTranslationResult.text = result
                    showTranslationActions()
                } else {
                    binding.tvTranslationResult.text = "Translation failed. Please check your internet connection and try again."
                    hideTranslationActions()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvTranslationResult.text = "Translation service unavailable. Please check your internet connection."
                hideTranslationActions()
            }
        }
    }

    private fun clearTranslation() {
        binding.tvTranslationResult.text = ""
        hideTranslationActions()
    }

    private fun showTranslationActions() {
        binding.btnCopyTranslation.visibility = View.VISIBLE
        binding.btnSpeakTranslation.visibility = View.VISIBLE
    }

    private fun hideTranslationActions() {
        binding.btnCopyTranslation.visibility = View.GONE
        binding.btnSpeakTranslation.visibility = View.GONE
    }

    private fun retranslateIfNeeded() {
        val text = binding.etInputText.text.toString()
        if (text.isNotEmpty()) {
            translateText(text)
        }
    }

    private fun swapLanguages() {
        // Swap language names
        val tempName = sourceLanguageName
        sourceLanguageName = targetLanguageName
        targetLanguageName = tempName

        // Swap texts
        val inputText = binding.etInputText.text.toString()
        val translationText = binding.tvTranslationResult.text.toString()

        if (translationText.isNotEmpty() &&
            translationText != "Translating..." &&
            !translationText.contains("failed") &&
            !translationText.contains("unavailable")) {

            binding.etInputText.setText(translationText)
            binding.tvTranslationResult.text = inputText
        }

        // Update UI
        updateLanguageDisplay()
    }

    private fun copyTranslation() {
        val translationText = binding.tvTranslationResult.text.toString()
        if (translationText.isNotEmpty() &&
            !translationText.contains("failed") &&
            !translationText.contains("unavailable") &&
            translationText != "Translating...") {

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Translation", translationText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Translation copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakTranslation() {
        val translationText = binding.tvTranslationResult.text.toString()
        if (translationText.isNotEmpty() &&
            !translationText.contains("failed") &&
            !translationText.contains("unavailable") &&
            translationText != "Translating..." &&
            textToSpeech != null) {

            // Set language for TTS
            val locale = getLocaleForLanguage(targetLanguageName)
            textToSpeech?.language = locale
            textToSpeech?.speak(translationText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun getLocaleForLanguage(language: String): Locale {
        return when (language) {
            "Vietnamese" -> Locale("vi", "VN")
            "English" -> Locale.ENGLISH
            "Chinese" -> Locale.CHINESE
            "Japanese" -> Locale.JAPANESE
            "Korean" -> Locale.KOREAN
            "French" -> Locale.FRENCH
            "German" -> Locale.GERMAN
            "Spanish" -> Locale("es", "ES")
            "Thai" -> Locale("th", "TH")
            "Russian" -> Locale("ru", "RU")
            "Italian" -> Locale.ITALIAN
            "Portuguese" -> Locale("pt", "PT")
            "Arabic" -> Locale("ar", "SA")
            "Hindi" -> Locale("hi", "IN")
            else -> Locale.ENGLISH
        }
    }

    private fun showLanguageSelector(isSource: Boolean, onLanguageSelected: (String) -> Unit) {
        val currentLanguage = if (isSource) sourceLanguageName else targetLanguageName
        val availableLanguages = languageNames.filter {
            if (isSource) it != targetLanguageName else it != sourceLanguageName
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Select Language")

        val currentIndex = availableLanguages.indexOf(currentLanguage)
        builder.setSingleChoiceItems(availableLanguages.toTypedArray(), currentIndex) { dialog, which ->
            onLanguageSelected(availableLanguages[which])
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.ENGLISH
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.shutdown()
    }
}