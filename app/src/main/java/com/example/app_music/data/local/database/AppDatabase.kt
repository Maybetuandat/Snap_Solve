package com.example.app_music.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.app_music.data.local.database.dao.UserCredentialsDao
import com.example.app_music.data.local.database.entity.UserCredentials

@Database(entities = [UserCredentials::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userCredentialsDao(): UserCredentialsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,   //context la context cua appilcation
                    AppDatabase::class.java,
                    "snap_solve_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}