package com.example.app_music.data.collaboration

import android.graphics.Path
import android.util.Log
import android.view.MotionEvent
import com.example.app_music.presentation.feature.noteScene.views.DrawingView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

/**
 * Manages real-time collaboration for note editing
 */
class CollaborationManager(private val noteId: String) {
    
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: UUID.randomUUID().toString()
    
    private val noteRef = database.getReference("note_edits").child(noteId)
    private val usersRef = database.getReference("note_users").child(noteId)
    private val drawingRef = database.getReference("note_drawings").child(noteId)
    
    // User presence data
    private val userPresenceRef = usersRef.child(currentUserId)

    private val connectionRef = database.getReference(".info/connected")
    private var lastUsername: String = ""
    private var lastColor: Int = 0
    data class DrawingAction(
        val userId: String = "",
        val actionId: String = "",
        val type: ActionType = ActionType.DRAW,
        val path: List<PathPoint> = emptyList(),
        val color: Int = 0,
        val strokeWidth: Float = 5f,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class PathPoint(
        val x: Float = 0f,
        val y: Float = 0f,
        val operation: PathOperation = PathOperation.MOVE_TO
    )
    
    data class UserInfo(
        val userId: String = "",
        val username: String = "",
        val color: Int = 0,
        val lastActive: Long = 0,
        val isTyping: Boolean = false
    )
    
    enum class ActionType {
        DRAW, ERASE, CLEAR
    }
    
    enum class PathOperation {
        MOVE_TO, LINE_TO, QUAD_TO
    }
    
    init {
        // Set up disconnect handler to mark user as offline when disconnected
        userPresenceRef.onDisconnect().removeValue()
    }
    
    /**
     * Mark the current user as present/active
     */
    init {
        // Set up disconnect handler
        userPresenceRef.onDisconnect().removeValue()

        // Monitor connection state
        connectionRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    Log.d(TAG, "Connected to Firebase Realtime Database")
                    // Reset user presence when reconnected
                    setUserPresence(lastUsername, lastColor)
                } else {
                    Log.d(TAG, "Disconnected from Firebase Realtime Database")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error monitoring connection state", error.toException())
            }
        })
    }
    fun setUserPresence(username: String, color: Int) {
        lastUsername = username
        lastColor = color

        val userInfo = UserInfo(
            userId = currentUserId,
            username = username,
            color = color,
            lastActive = System.currentTimeMillis(),
            isTyping = false
        )
        userPresenceRef.setValue(userInfo)
            .addOnSuccessListener {
                Log.d(TAG, "User presence set successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error setting user presence", e)
            }

        // Keep user presence active with server timestamp
        userPresenceRef.child("lastActive").setValue(ServerValue.TIMESTAMP)
    }
    
    /**
     * Update user typing status
     */
    fun setUserTyping(isTyping: Boolean) {
        userPresenceRef.child("isTyping").setValue(isTyping)
        userPresenceRef.child("lastActive").setValue(System.currentTimeMillis())
    }
    fun strokeToDrawingAction(stroke: DrawingView.Stroke): DrawingAction {
        val pathPoints = stroke.points.map { point ->
            PathPoint(
                x = point.x,
                y = point.y,
                operation = when (point.type) {
                    MotionEvent.ACTION_DOWN -> PathOperation.MOVE_TO
                    MotionEvent.ACTION_MOVE -> PathOperation.LINE_TO
                    MotionEvent.ACTION_UP -> PathOperation.LINE_TO
                    else -> PathOperation.LINE_TO
                }
            )
        }

        return DrawingAction(
            type = if (stroke.isEraser) ActionType.ERASE else ActionType.DRAW,
            path = pathPoints,
            color = stroke.color,
            strokeWidth = stroke.strokeWidth
        )
    }
    /**
     * Remove user presence when they leave
     */
    fun removeUserPresence() {
        userPresenceRef.removeValue()
    }
    fun cleanup() {
        // Hủy đăng ký tất cả các listener
        try {
            userPresenceRef.removeValue()
            // Hủy các listener khác nếu cần
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi dọn dẹp CollaborationManager", e)
        }
    }

    /**
     * Get active users as a flow
     */
    fun getActiveUsers(): Flow<List<UserInfo>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<UserInfo>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(UserInfo::class.java)
                    if (user != null) {
                        users.add(user)
                    }
                }
                trySend(users)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error getting active users", error.toException())
            }
        }
        
        usersRef.addValueEventListener(listener)
        
        awaitClose {
            usersRef.removeEventListener(listener)
        }
    }
    
    /**
     * Save a drawing action to Firebase
     */
    fun saveDrawingAction(action: DrawingAction) {
        val actionId = action.actionId.ifEmpty { 
            database.reference.push().key ?: UUID.randomUUID().toString() 
        }
        
        val actionWithIds = action.copy(
            userId = currentUserId,
            actionId = actionId,
            timestamp = System.currentTimeMillis()
        )
        
        drawingRef.child(actionId).setValue(actionWithIds)
    }
    
    /**
     * Convert a Path to a serializable list of PathPoints
     */
    fun pathToPointsList(path: Path): List<PathPoint> {
        // This is simplified - in reality you'd need a custom path 
        // recorder to capture all path operations
        // For now, return an empty list as a placeholder
        return emptyList()
    }
    
    /**
     * Get all drawing actions as a flow
     */
    fun getDrawingActions(): Flow<List<DrawingAction>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val actions = mutableListOf<DrawingAction>()
                for (actionSnapshot in snapshot.children) {
                    val action = actionSnapshot.getValue(DrawingAction::class.java)
                    if (action != null) {
                        actions.add(action)
                    }
                }
                trySend(actions)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error getting drawing actions", error.toException())
            }
        }
        
        drawingRef.addValueEventListener(listener)
        
        awaitClose {
            drawingRef.removeEventListener(listener)
        }
    }
    
    companion object {
        private const val TAG = "CollaborationManager"
    }
}