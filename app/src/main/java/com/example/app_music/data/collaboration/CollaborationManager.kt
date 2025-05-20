package com.example.app_music.data.collaboration

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
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
    private lateinit var connectionListener: ValueEventListener
    private val usersRef = database.getReference("note_users").child(noteId)
    private val drawingRef = database.getReference("note_drawings").child(noteId)

    // User presence data
    private val userPresenceRef = usersRef.child(currentUserId)
    private val connectionRef = database.getReference(".info/connected")
    private var lastUsername: String = ""
    private var lastColor: Int = 0

    // Timer variables - initialize handler right away
    private val updateHandler = Handler(Looper.getMainLooper())
    private var activeUpdateRunnable: Runnable? = null

    data class DrawingAction(
        val userId: String = "",
        val actionId: String = "",
        val type: ActionType = ActionType.DRAW,
        val path: List<PathPoint> = emptyList(),
        val color: Int = 0,
        val strokeWidth: Float = 5f,
        val timestamp: Long = System.currentTimeMillis(),
        val pageId: String = "", // Add pageId field
        val targetStrokeId: String = "" // For erase actions
    )

    fun getCurrentUserId(): String {
        return currentUserId
    }

    enum class PageEventType {
        PAGE_ADDED,
        PAGE_DELETED,
        PAGES_REORDERED
    }

    // Create PageEvent class with no-argument constructor
    data class PageEvent(
        val type: String = "PAGE_ADDED",  // Store as String to avoid enum serialization issues
        val noteId: String = "",
        val pageId: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val userId: String = "",
        val pageIndex: Int = -1,
        val allPageIds: List<String> = emptyList()
    ) {
        // No-argument constructor required by Firebase
        constructor() : this(
            "PAGE_ADDED",
            "",
            "",
            System.currentTimeMillis(),
            "",
            -1,
            emptyList()
        )

        // Helper method to convert string to enum
        fun getEventType(): PageEventType {
            return when(type) {
                "PAGE_ADDED" -> PageEventType.PAGE_ADDED
                "PAGE_DELETED" -> PageEventType.PAGE_DELETED
                "PAGES_REORDERED" -> PageEventType.PAGES_REORDERED
                else -> PageEventType.PAGE_ADDED // Default
            }
        }
    }


    // Phương thức dọn dẹp người dùng cũ
    private fun cleanupStaleUsers() {
        val cutoffTime = System.currentTimeMillis() - 60000 // 1 phút

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key ?: return@forEach
                    val lastActive = userSnapshot.child("lastActive").getValue(Long::class.java) ?: 0L

                    // Nếu không hoạt động quá 1 phút và không phải là người dùng hiện tại
                    if (lastActive < cutoffTime && userId != currentUserId) {
                        usersRef.child(userId).removeValue()
                            .addOnSuccessListener {
                                Log.d(TAG, "Removed stale user: $userId")
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error cleanup stale users", error.toException())
            }
        })
    }

    // Phương thức tìm và xóa người dùng trùng lặp
    private fun findAndRemoveDuplicateUsers() {
        // Tìm tất cả phiên kết nối của cùng một người dùng
        usersRef.orderByChild("username").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usernames = mutableMapOf<String, MutableList<Pair<String, Long>>>()

                // Tìm tất cả người dùng có cùng tên
                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key ?: return@forEach
                    val username = userSnapshot.child("username").getValue(String::class.java) ?: return@forEach
                    val lastActive = userSnapshot.child("lastActive").getValue(Long::class.java) ?: 0L

                    if (!usernames.containsKey(username)) {
                        usernames[username] = mutableListOf()
                    }

                    usernames[username]?.add(Pair(userId, lastActive))
                }

                // Xóa tất cả các phiên cũ trừ phiên mới nhất và phiên hiện tại
                usernames.forEach { (_, userEntries) ->
                    if (userEntries.size > 1) {
                        // Sắp xếp theo thời gian hoạt động gần đây nhất
                        val sortedEntries = userEntries.sortedByDescending { it.second }

                        // Giữ lại phiên mới nhất và phiên hiện tại, xóa phần còn lại
                        sortedEntries.forEachIndexed { index, (userId, _) ->
                            if (index > 0 && userId != currentUserId) {
                                usersRef.child(userId).removeValue()
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Removed duplicate user: $userId")
                                    }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error finding duplicate users", error.toException())
            }
        })
    }

    // Update emitPageEvent function to use string representation
    fun emitPageEvent(eventType: PageEventType, noteId: String, pageId: String, pageIndex: Int = -1, allPageIds: List<String> = emptyList()) {
        val event = PageEvent(
            type = eventType.name, // Store enum as string
            noteId = noteId,
            pageId = pageId,
            timestamp = System.currentTimeMillis(),
            userId = currentUserId,
            pageIndex = pageIndex,
            allPageIds = allPageIds
        )
        pageEventsRef.push().setValue(event)
    }

    private val pageEventsRef = database.getReference("note_page_events").child(noteId)

    // Function to emit page events
    fun emitPageEvent(event: PageEvent) {
        val eventWithUser = event.copy(userId = currentUserId)
        pageEventsRef.push().setValue(eventWithUser)
    }

    // Function to listen for page events
    fun getPageEventsFlow(): Flow<PageEvent> = callbackFlow {
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val event = snapshot.getValue(PageEvent::class.java)
                if (event != null && event.userId != currentUserId) {
                    trySend(event)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Page events listener cancelled", error.toException())
            }
        }

        pageEventsRef.addChildEventListener(listener)

        awaitClose {
            pageEventsRef.removeEventListener(listener)
        }
    }

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
        val isTyping: Boolean = false,
        val isOffline: Boolean = false  // Thêm trường này
    )

    enum class ActionType {
        DRAW, ERASE, CLEAR
    }

    enum class PathOperation {
        MOVE_TO, LINE_TO, QUAD_TO
    }

    init {
        // Xóa người dùng cũ khi khởi tạo
        cleanupStaleUsers()

        // Set up disconnect handler to mark user as offline when disconnected
        userPresenceRef.onDisconnect().removeValue()
        userPresenceRef.child("lastActive").onDisconnect().setValue(ServerValue.TIMESTAMP)

        // Đánh dấu trạng thái offline khi ngắt kết nối
        userPresenceRef.child("isOffline").onDisconnect().setValue(true)
        // Monitor connection state
        connectionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    Log.d(TAG, "Connected to Firebase Realtime Database")

                    // Xóa bất kỳ người dùng trùng lặp nào có thể tồn tại
                    findAndRemoveDuplicateUsers()

                    // Đặt một giá trị server timestamp để kiểm tra phiên kết nối
                    userPresenceRef.child("connectionTime").setValue(ServerValue.TIMESTAMP)

                    // Reset user presence when reconnected
                    setUserPresence(lastUsername, lastColor)
                } else {
                    Log.d(TAG, "Disconnected from Firebase Realtime Database")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error monitoring connection state", error.toException())
            }
        }

        connectionRef.addValueEventListener(connectionListener)
    }

    fun setUserPresence(username: String, color: Int) {
        lastUsername = username
        lastColor = color

        val userInfo = UserInfo(
            userId = currentUserId,
            username = username,
            color = color,
            lastActive = System.currentTimeMillis(),
            isTyping = false,
            isOffline = false  // Thêm trạng thái offline là false khi active
        )
        userPresenceRef.setValue(userInfo)
            .addOnSuccessListener {
                Log.d(TAG, "User presence set successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error setting user presence", e)
            }
    }

    private fun stopActiveUpdates() {
        // Xóa bỏ callback an toàn
        activeUpdateRunnable?.let {
            updateHandler.removeCallbacks(it)
            activeUpdateRunnable = null
        }
    }

    /**
     * Update user typing status
     */
    fun setUserTyping(isTyping: Boolean) {
        userPresenceRef.child("isTyping").setValue(isTyping)
        userPresenceRef.child("lastActive").setValue(System.currentTimeMillis())
    }


    fun removeUserPresence() {
        userPresenceRef.removeValue()
    }

    fun cleanup() {
        try {
            // Hủy timer cập nhật lastActive - cách an toàn
            stopActiveUpdates()

            // Chủ động xóa hiện diện người dùng, không chỉ dựa vào onDisconnect
            userPresenceRef.removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "User presence removed successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error removing user presence", e)
                }

            // Cancel any pending listeners
            connectionRef.removeEventListener(connectionListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error during collaboration manager cleanup", e)
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
                        // Lọc các người dùng không hoạt động quá 2 phút
                        val currentTime = System.currentTimeMillis()
                        val timeoutThreshold = 120000 // 2 phút timeout

                        if (currentTime - user.lastActive < timeoutThreshold || user.userId == currentUserId) {
                            users.add(user)
                        } else {
                            // Xóa người dùng không hoạt động
                            usersRef.child(user.userId).removeValue()
                                .addOnSuccessListener {
                                    Log.d(TAG, "Auto-removed inactive user: ${user.userId}")
                                }
                        }
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


    companion object {
        private const val TAG = "CollaborationManager"
    }

}