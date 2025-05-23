package com.example.app_music.data.repository

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.presentation.feature.noteScene.views.DrawingView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class StrokesRepository(private val context: Context) {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Sử dụng Realtime Database cho strokes để đồng bộ
    private val strokesRef = database.getReference("strokes")

    private val currentUserId: String
        get() = UserPreference.getUserId(context).toString()

    suspend fun saveStroke(pageId: String, stroke: DrawingView.Stroke): Result<String> {
        return try {
            val strokeData = mapOf(
                "id" to stroke.id,
                "pageId" to pageId,
                "color" to stroke.color,
                "strokeWidth" to stroke.strokeWidth,
                "isEraser" to stroke.isEraser,
                "points" to stroke.points.map { point ->
                    mapOf(
                        "x" to point.x,  // lưu tọa độ chuẩn hóa (0.0-1.0)
                        "y" to point.y,  // lưu tọa độ chuẩn hóa (0.0-1.0)
                        "type" to point.type
                    )
                },
                "createdAt" to System.currentTimeMillis(),
                "createdBy" to currentUserId
            )

            // Lưu vào Realtime Database
            val strokeRef = strokesRef.child(pageId).child(stroke.id)
            strokeRef.setValue(strokeData).await()
            Result.success(stroke.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stroke", e)
            Result.failure(e)
        }
    }

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
                                // Đọc tọa độ đã chuẩn hóa
                                val x = pointSnapshot.child("x").getValue(Double::class.java)?.toFloat() ?: 0f
                                val y = pointSnapshot.child("y").getValue(Double::class.java)?.toFloat() ?: 0f
                                val type = pointSnapshot.child("type").getValue(Int::class.java) ?: 0

                                points.add(DrawingView.StrokePoint(x, y, type))
                            }

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

    companion object {
        private const val TAG = "StrokesRepository"
    }
}