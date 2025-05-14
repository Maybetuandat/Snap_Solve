package com.example.app_music.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.app_music.data.model.UserDto
import com.google.gson.Gson

class PreferencesDataSource(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
    private val gson = Gson()

    companion object {
        private const val SHARED_PREF_NAME = "snap_solve_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_data"
        private const val KEY_TEMP_PASSWORD = "temp_password"
    }

    /**
     * Save authentication token
     */
    fun saveAuthToken(token: String) {
        editor.putString(KEY_TOKEN, token)
        editor.apply()
    }

    /**
     * Get saved authentication token
     */
    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    /**
     * Save temporary password (for auto-login after registration)
     */
    fun saveTempPassword(password: String) {
        editor.putString(KEY_TEMP_PASSWORD, password)
        editor.apply()
    }

    /**
     * Get stored temporary password
     */
    fun getTempPassword(): String? {
        return sharedPreferences.getString(KEY_TEMP_PASSWORD, null)
    }

    /**
     * Save user data
     */
    fun saveUserData(user: UserDto) {
        val userJson = gson.toJson(user)
        editor.putString(KEY_USER, userJson)
        editor.apply()
    }

    /**
     * Get saved user data
     */
    fun getUserData(): UserDto? {
        val userJson = sharedPreferences.getString(KEY_USER, null)
        return if (userJson != null) {
            gson.fromJson(userJson, UserDto::class.java)
        } else {
            null
        }
    }

    /**
     * Clear all user session data
     */
    fun clearAll() {
        editor.clear().apply()
    }
}