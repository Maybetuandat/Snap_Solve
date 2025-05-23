package com.example.app_music.presentation.feature.translate

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GoogleTranslateService {

    companion object {
        private const val TAG = "GoogleTranslateService"
        private const val GOOGLE_TRANSLATE_URL = "https://translate.googleapis.com/translate_a/single"
        private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
        private const val READ_TIMEOUT = 15000 // 15 seconds
    }

    suspend fun translateText(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Encode the text for URL
            val encodedText = URLEncoder.encode(text, "UTF-8")

            // Build the URL with parameters
            val urlString = buildString {
                append(GOOGLE_TRANSLATE_URL)
                append("?client=gtx")
                append("&sl=").append(sourceLanguage)
                append("&tl=").append(targetLanguage)
                append("&dt=t")
                append("&q=").append(encodedText)
            }

            Log.d(TAG, "Translating from $sourceLanguage to $targetLanguage")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            // Set connection properties
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECTION_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Accept-Encoding", "gzip, deflate, br")
                setRequestProperty("Connection", "keep-alive")
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val response = reader.use { it.readText() }

                Log.d(TAG, "Raw response: ${response.take(200)}...")

                // Parse the response
                val translatedText = parseTranslationResponse(response)
                Log.d(TAG, "Translated text: $translatedText")

                translatedText
            } else {
                Log.e(TAG, "HTTP Error: $responseCode")
                // Try to read error response
                val errorStream = connection.errorStream
                if (errorStream != null) {
                    val errorReader = BufferedReader(InputStreamReader(errorStream))
                    val errorResponse = errorReader.use { it.readText() }
                    Log.e(TAG, "Error response: $errorResponse")
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Translation error", e)
            null
        }
    }

    private fun parseTranslationResponse(response: String): String? {
        return try {
            // Google Translate API returns a complex nested array structure like:
            // [[["translated text","original text",null,null,3,null,null,[],[[["en"],null,[["en"],["de"]]]]],null,"vi",null,null,null,null,[]]

            if (!response.startsWith("[")) {
                Log.e(TAG, "Invalid response format")
                return null
            }

            // Find the first translated text segment
            var result = StringBuilder()
            var inQuotes = false
            var escapeNext = false
            var bracketCount = 0
            var segmentCount = 0

            for (i in response.indices) {
                val char = response[i]

                when {
                    escapeNext -> {
                        if (inQuotes) result.append(char)
                        escapeNext = false
                    }
                    char == '\\' -> {
                        escapeNext = true
                        if (inQuotes) result.append(char)
                    }
                    char == '"' && bracketCount >= 3 -> {
                        if (inQuotes) {
                            // End of a quoted segment
                            segmentCount++
                            if (segmentCount == 1) {
                                // This is our translation
                                return result.toString()
                            }
                            result.clear()
                        }
                        inQuotes = !inQuotes
                    }
                    char == '[' -> {
                        bracketCount++
                    }
                    char == ']' -> {
                        bracketCount--
                    }
                    inQuotes -> {
                        result.append(char)
                    }
                }
            }

            // If we couldn't parse properly, try a simpler approach
            return parseSimple(response)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing translation response", e)
            parseSimple(response)
        }
    }

    private fun parseSimple(response: String): String? {
        return try {
            // Fallback: Look for the first quoted string after the opening brackets
            val pattern = Regex("\\[\\[\\[\"([^\"]+)\"")
            val matchResult = pattern.find(response)
            val result = matchResult?.groupValues?.get(1)

            // Unescape common characters
            result?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")

        } catch (e: Exception) {
            Log.e(TAG, "Error in simple parsing", e)
            null
        }
    }
}