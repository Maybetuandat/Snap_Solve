package com.example.app_music.domain.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HMacUtil {
    const val HMACMD5 = "HmacMD5"
    const val HMACSHA1 = "HmacSHA1"
    const val HMACSHA256 = "HmacSHA256"
    const val HMACSHA512 = "HmacSHA512"
    val UTF8CHARSET = StandardCharsets.UTF_8

    private fun hMacEncode(algorithm: String, key: String, data: String): ByteArray? {
        return try {
            val macGenerator = Mac.getInstance(algorithm)
            val signingKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), algorithm)
            macGenerator.init(signingKey)
            macGenerator.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        } catch (ex: Exception) {
            null
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun hMacBase64Encode(algorithm: String, key: String, data: String): String? {
        val hmacEncodeBytes = hMacEncode(algorithm, key, data) ?: return null
        return Base64.getEncoder().encodeToString(hmacEncodeBytes)
    }

    fun HMacHexStringEncode(algorithm: String, key: String, data: String): String? {
        val hmacEncodeBytes = hMacEncode(algorithm, key, data) ?: return null
        return HexStringUtil.byteArrayToHexString(hmacEncodeBytes)
    }
}