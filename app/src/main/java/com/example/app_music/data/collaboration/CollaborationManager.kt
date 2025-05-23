package com.example.app_music.data.collaboration

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.app_music.data.local.preferences.UserPreference
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

class CollaborationManager(private val noteId: String, private val context: Context) {
    //khoi tao firebase realtime database
    private val database = FirebaseDatabase.getInstance()
    private val currentUserId = UserPreference.getUserId(context).toString()

    //lang nghe du lieu tu firebase
    private lateinit var connectionListener: ValueEventListener

    private val usersRef = database.getReference("note_users").child(noteId)

    //cấu trúc bảng note_users
    data class UserInfo(
        val userId: String = "",
        val username: String = "",
        val color: Int = 0,
        val lastActive: Long = 0,
        val isTyping: Boolean = false,
        val isOffline: Boolean = false
    )

    private val userPresenceRef = usersRef.child(currentUserId)

    //check xem trạng thái kết nối firebase thế nào, gọi khi tắt mạng hoặc bật mạng trở lại
    private val connectionRef = database.getReference(".info/connected")

    private var lastUsername: String = ""
    private var lastColor: Int = 0

    fun getCurrentUserId(): String {
        return currentUserId
    }

    enum class PageEventType {
        PAGE_ADDED,
        PAGE_DELETED,
        PAGES_REORDERED
    }

    // dọn dẹp người dùng cũ
    private fun cleanupStaleUsers() {
        val cutoffTime = System.currentTimeMillis() - 60000 // 60s

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key ?: return@forEach
                    val lastActive = userSnapshot.child("lastActive").getValue(Long::class.java) ?: 0L

                    // khong hoat dong qua 1p va khong phai nguoi dung hien tai thi remove
                    if (lastActive < cutoffTime && userId != currentUserId) {
                        usersRef.child(userId).removeValue()
                        Log.d("Collaboration", "Xóa " + userId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    // tìm và xóa người dùng trùng lặp
    private fun findAndRemoveDuplicateUsers() {
        usersRef.orderByChild("username").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usernames = mutableMapOf<String, MutableList<Pair<String, Long>>>()

                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key ?: return@forEach
                    val username = userSnapshot.child("username").getValue(String::class.java) ?: return@forEach
                    val lastActive = userSnapshot.child("lastActive").getValue(Long::class.java) ?: 0L

                    if (!usernames.containsKey(username)) {
                        usernames[username] = mutableListOf()
                    }

                    usernames[username]?.add(Pair(userId, lastActive))
                }

                usernames.forEach { (_, userEntries) ->
                    if (userEntries.size > 1) {
                        // Sắp xếp theo thời gian hoạt động gần đây nhất
                        val sortedEntries = userEntries.sortedByDescending { it.second }

                        // Giữ lại phiên mới nhất và phiên của người dùng hiện tại, xóa phần còn lại
                        sortedEntries.forEachIndexed { index, (userId, _) ->
                            if (index > 0 && userId != currentUserId) {
                                usersRef.child(userId).removeValue()
                                Log.d("Collaboration", "Xóa " + userId)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private val pageEventsRef = database.getReference("note_page_events").child(noteId)

    //liên quan đến bảng note_page_events
    data class PageEvent(
        val type: String = "PAGE_ADDED",
        val noteId: String = "",
        val pageId: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val userId: String = "",
        val pageIndex: Int = -1,
        val allPageIds: List<String> = emptyList()
    ) {
        constructor() : this(
            "PAGE_ADDED",
            "",
            "",
            System.currentTimeMillis(),
            "",
            -1,
            emptyList()
        )
        fun getEventType(): PageEventType {
            return when(type) {
                "PAGE_ADDED" -> PageEventType.PAGE_ADDED
                "PAGE_DELETED" -> PageEventType.PAGE_DELETED
                "PAGES_REORDERED" -> PageEventType.PAGES_REORDERED
                else -> PageEventType.PAGE_ADDED
            }
        }
    }

    // Cấu trúc bảng note_page_events
    fun emitPageEvent(eventType: PageEventType, noteId: String, pageId: String, pageIndex: Int = -1, allPageIds: List<String> = emptyList()) {
        val event = PageEvent(
            type = eventType.name,
            noteId = noteId,
            pageId = pageId,
            timestamp = System.currentTimeMillis(),
            userId = currentUserId,
            pageIndex = pageIndex,
            allPageIds = allPageIds
        )
        pageEventsRef.push().setValue(event)
    }

    // lắng nghe page_events, trả ra PageEvent
    fun getPageEventsFlow(): Flow<PageEvent> = callbackFlow {
        //lắng nghe sự kiện của firebase
        val listener = object : ChildEventListener {
            //sự kiện thêm vào data trong firebase
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val event = snapshot.getValue(PageEvent::class.java)
                if (event != null && event.userId != currentUserId) {
                    //gửi event đi -> bên NoteDetailActivity nghe
                    trySend(event)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
            }
        }

        pageEventsRef.addChildEventListener(listener)

        awaitClose {
            pageEventsRef.removeEventListener(listener)
        }
    }

    init {
        // Xóa người dùng cũ khi khởi tạo
        cleanupStaleUsers()

        // Nếu người dùng không connect thì remove đi
        userPresenceRef.onDisconnect().removeValue()
        //nếu disconnect thì đặt các trạng thái
        userPresenceRef.child("lastActive").onDisconnect().setValue(ServerValue.TIMESTAMP)
        userPresenceRef.child("isOffline").onDisconnect().setValue(true)

        // Điều khiển các kết nối
        connectionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    findAndRemoveDuplicateUsers()
                    // Đặt thời gian kết nối cho người dùng
                    userPresenceRef.child("connectionTime").setValue(ServerValue.TIMESTAMP)
                    //set lại giá trị khi connect
                    setUserPresence(lastUsername, lastColor)
                }
            }

            override fun onCancelled(error: DatabaseError) {
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
            isOffline = false
        )
        userPresenceRef.setValue(userInfo)
    }

    //Cập nhật trạng thái typing
    fun setUserTyping(isTyping: Boolean) {
        userPresenceRef.child("isTyping").setValue(isTyping)
        userPresenceRef.child("lastActive").setValue(System.currentTimeMillis())
    }

    fun removeUserPresence() {
        userPresenceRef.removeValue()
    }

    fun cleanup() {
        try {
            // Xóa người dùng hiện tại
            userPresenceRef.removeValue()

            connectionRef.removeEventListener(connectionListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error during collaboration manager cleanup", e)
        }
    }

    fun getActiveUsers(): Flow<List<UserInfo>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<UserInfo>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(UserInfo::class.java)
                    if (user != null) {
                        // Lọc các người dùng không hoạt động quá 2 phút
                        val currentTime = System.currentTimeMillis()
                        val timeoutThreshold = 120000 // 2 phút
                        Log.d("Collaboration", user.userId)

                        if (currentTime - user.lastActive < timeoutThreshold) {
                            users.add(user)
                        } else {
                            // Xóa người dùng không hoạt động
                            usersRef.child(user.userId).removeValue()
                            Log.d("Collaboration", "Xóa " + user.userId)
                        }
                    }
                }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
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