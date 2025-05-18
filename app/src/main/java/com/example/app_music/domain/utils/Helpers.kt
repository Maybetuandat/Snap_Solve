package com.example.app_music.domain.utils

import android.annotation.SuppressLint
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Helpers {
    private var transIdDefault = 1

    @SuppressLint("DefaultLocale")
    fun getAppTransId(): String {
        if (transIdDefault >= 100000) {
            transIdDefault = 1
        }
        transIdDefault += 1
        val formatDateTime = SimpleDateFormat("yyMMdd_hhmmss", Locale.getDefault())
        val timeString = formatDateTime.format(Date())
        return String.format("%s%06d", timeString, transIdDefault)
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun getMac(key: String, data: String): String {
        return HMacUtil.HMacHexStringEncode(HMacUtil.HMACSHA256, key, data)
            ?: throw IllegalStateException("Failed to generate MAC")
    }
}
