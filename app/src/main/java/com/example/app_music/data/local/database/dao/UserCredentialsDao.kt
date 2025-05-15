package com.example.app_music.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app_music.data.local.database.entity.UserCredentials

@Dao
interface UserCredentialsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCredentials(credentials: UserCredentials)

    @Query("SELECT * FROM user_credentials WHERE isRemembered = 1 LIMIT 1")
    suspend fun getRememberedCredentials(): UserCredentials?

    @Query("DELETE FROM user_credentials")
    suspend fun deleteAllCredentials()
}