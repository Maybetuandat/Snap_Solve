package com.example.app_music.data.remote.api
import okhttp3.*

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object HttpProvider {
    fun sendPost(url: String, formBody: RequestBody): JSONObject {
        return try {
            val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .cipherSuites(
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
                )
                .build()

            val client = OkHttpClient.Builder()
                .connectionSpecs(listOf(spec))
                .callTimeout(5000, TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e("BAD_REQUEST", response.body?.string() ?: "Unknown error")
                JSONObject()
            } else {
                JSONObject(response.body?.string() ?: "{}")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            JSONObject()
        } catch (e: JSONException) {
            e.printStackTrace()
            JSONObject()
        }
    }
}