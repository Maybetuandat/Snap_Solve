package com.example.app_music

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage

class SnapSolveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo Firebase
        FirebaseApp.initializeApp(this)

        //Cấu hình firebase, tự động cache dữ liệu để sử dụng offline
        // Cache mặc định là 40MB
        val firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings

        // Khởi tạo Storage
        FirebaseStorage.getInstance()
    }
}