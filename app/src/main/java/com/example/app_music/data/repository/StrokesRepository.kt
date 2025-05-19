package com.example.app_music.data.repository

import android.graphics.Color
import android.util.Log
import com.example.app_music.data.model.NotePage
import com.example.app_music.presentation.feature.noteScene.views.DrawingView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository riêng biệt để quản lý các nét vẽ trên Firebase Realtime Database
 */
class StrokesRepository {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Sử dụng Realtime Database cho strokes để đồng bộ tốt hơn
    private val strokesRef = database.getReference("strokes")
    
    // ID người dùng hiện tại
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: "test_user_1"
    
    /**
     * Lưu một nét vẽ mới
     */
    suspend fun saveStroke(pageId: String, stroke: DrawingView.Stroke): Result<String> {
        return try {
            // Tạo map dữ liệu cho stroke
            val strokeData = mapOf(
                "id" to stroke.id,
                "pageId" to pageId,
                "color" to stroke.color,
                "strokeWidth" to stroke.strokeWidth,
                "isEraser" to stroke.isEraser,
                "points" to stroke.points,
                "createdAt" to System.currentTimeMillis(),
                "createdBy" to currentUserId
            )
            
            // Lưu vào Realtime Database
            val strokeRef = strokesRef.child(pageId).child(stroke.id)
            strokeRef.setValue(strokeData).await()
            
            // Không cần cập nhật NotePage vì chúng ta sẽ lấy trực tiếp từ DB
            
            Result.success(stroke.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stroke", e)
            Result.failure(e)
        }
    }
    
    /**
     * Lấy tất cả các nét vẽ cho một trang sử dụng coroutines
     */
    suspend fun getStrokesForPage(pageId: String): Result<List<DrawingView.Stroke>> = suspendCancellableCoroutine { continuation ->
        val pageStrokesRef = strokesRef.child(pageId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val strokes = mutableListOf<DrawingView.Stroke>()
                    
                    for (strokeSnapshot in snapshot.children) {
                        try {
                            val id = strokeSnapshot.child("id").getValue(String::class.java) ?: continue
                            val color = strokeSnapshot.child("color").getValue(Long::class.java)?.toInt() ?: Color.BLACK
                            val strokeWidth = strokeSnapshot.child("strokeWidth").getValue(Double::class.java)?.toFloat() ?: 5f
                            val isEraser = strokeSnapshot.child("isEraser").getValue(Boolean::class.java) ?: false
                            
                            // Get points data
                            val pointsSnapshot = strokeSnapshot.child("points")
                            val points = mutableListOf<DrawingView.StrokePoint>()
                            
                            for (pointSnapshot in pointsSnapshot.children) {
                                val x = pointSnapshot.child("x").getValue(Double::class.java)?.toFloat() ?: 0f
                                val y = pointSnapshot.child("y").getValue(Double::class.java)?.toFloat() ?: 0f
                                val type = pointSnapshot.child("type").getValue(Int::class.java) ?: 0
                                
                                points.add(DrawingView.StrokePoint(x, y, type))
                            }
                            
                            // Create stroke
                            val stroke = DrawingView.Stroke(
                                id = id,
                                color = color,
                                strokeWidth = strokeWidth,
                                isEraser = isEraser,
                                points = points
                            )
                            
                            strokes.add(stroke)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing stroke", e)
                        }
                    }
                    
                    // Sort by timestamp if available
                    val sortedStrokes = strokes.sortedBy { stroke -> 
                        snapshot.child(stroke.id).child("createdAt").getValue(Long::class.java) ?: 0L 
                    }
                    
                    continuation.resume(Result.success(sortedStrokes))
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                continuation.resume(Result.failure(error.toException()))
            }
        }
        
        pageStrokesRef.addListenerForSingleValueEvent(listener)
        
        continuation.invokeOnCancellation {
            pageStrokesRef.removeEventListener(listener)
        }
    }
    
    /**
     * Xóa một nét vẽ
     */
    suspend fun deleteStroke(pageId: String, strokeId: String): Result<Boolean> {
        return try {
            // Xóa stroke từ Realtime Database
            strokesRef.child(pageId).child(strokeId).removeValue().await()
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting stroke", e)
            Result.failure(e)
        }
    }
    
    /**
     * Xóa tất cả nét vẽ của một trang
     */
    suspend fun deleteAllStrokesForPage(pageId: String): Result<Boolean> {
        return try {
            // Xóa toàn bộ node của trang
            strokesRef.child(pageId).removeValue().await()
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all strokes for page", e)
            Result.failure(e)
        }
    }
    
    /**
     * Đếm số lượng nét vẽ trên một trang
     */
    suspend fun countStrokesForPage(pageId: String): Result<Int> = suspendCancellableCoroutine { continuation ->
        val pageStrokesRef = strokesRef.child(pageId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                continuation.resume(Result.success(count))
            }
            
            override fun onCancelled(error: DatabaseError) {
                continuation.resume(Result.failure(error.toException()))
            }
        }
        
        pageStrokesRef.addListenerForSingleValueEvent(listener)
        
        continuation.invokeOnCancellation {
            pageStrokesRef.removeEventListener(listener)
        }
    }
    
    companion object {
        private const val TAG = "StrokesRepository"
    }
}