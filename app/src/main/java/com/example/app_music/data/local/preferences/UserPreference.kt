package com.example.app_music.data.local.preferences

import android.content.Context

object UserPreference {

    private const val PREF_NAME = "user_preference"
    private const val USER_ID = "user_id"
    private const val USER_NAME = "user_name"  // Sửa tên hằng số cho đúng quy chuẩn

    fun saveUserId(context: Context, id: Long): Boolean {
        val preference = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preference.edit()
        editor.putString(USER_ID, id.toString())

        return editor.commit()
    }

    fun getUserId(context: Context): Long {
        val preference = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preference.getString(USER_ID, null)?.toLong() ?: 0
    }

    // Thêm phương thức để lưu user name
    fun saveUserName(context: Context, name: String): Boolean {
        val preference = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preference.edit()
        editor.putString(USER_NAME, name)

        return editor.commit()
    }

    // Thêm phương thức để lấy user name
    fun getUserName(context: Context): String {
        val preference = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preference.getString(USER_NAME, "") ?: ""
    }
    fun clearUserData(context: Context) {
        val preference = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preference.edit()
        editor.clear()
        editor.apply()
    }
}