package com.example.app_music.data.local.preferences

import android.content.Context

object UserPreference {

    private const val PREF_NAME= "user_preference"
    private  const val USER_ID = "user_id"

    fun saveUserId(context: Context, id: Long): Boolean
    {
        val preference = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preference.edit()
        editor.putString(USER_ID, id.toString())

        return editor.commit()
    }
    fun getUserId(context : Context):Long
    {
        val preference = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preference.getString(USER_ID, null)?.toLong() ?: 0
    }
}