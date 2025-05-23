package com.example.app_music.presentation.feature.noteScene.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.app_music.R
import com.example.app_music.data.collaboration.CollaborationManager.UserInfo

class UserPresenceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val userViewMap = mutableMapOf<String, View>()
    private val maxUsersVisible = 3
    private var currentUserId = ""

    fun setCurrentUserId(userId: String) {
        this.currentUserId = userId
    }

    fun updateActiveUsers(users: List<UserInfo>) {
        // Chỉ xét người dùng đang online
        val activeUsers = users.filter {
            !it.isOffline && it.userId != currentUserId
        }

        // Xóa bỏ người dùng không còn trong danh sách
        val currentUserIds = activeUsers.map { it.userId }
        val toRemove = userViewMap.keys.filter { it !in currentUserIds }

        toRemove.forEach { userId ->
            val view = userViewMap[userId] ?: return@forEach
            removeView(view)
            userViewMap.remove(userId)
        }

        // Cập nhật người dùng đang hoạt động
        activeUsers.take(maxUsersVisible).forEach { user ->
            if (user.userId in userViewMap) {
                // Cập nhật người dùng hiện tại
                updateUserView(user)
            } else {
                // Thêm người dùng mới
                addUserView(user)
            }
        }

        // Hiển thị số người dùng thêm nếu cần
        if (activeUsers.size > maxUsersVisible) {
            showMoreUsersCount(activeUsers.size - maxUsersVisible)
        } else {
            hideMoreUsersCount()
        }
    }

    private fun addUserView(user: UserInfo) {
        val view = LayoutInflater.from(context).inflate(R.layout.item_active_user, this, false)
        val indicator = view.findViewById<View>(R.id.user_color_indicator)
        val nameInitial = view.findViewById<TextView>(R.id.user_initial)

        // Đặt màu cho người dùng
        indicator.setBackgroundColor(user.color)

        // Đặt chữ cái đầu của tên người dùng, xử lý tên rỗng
        val username = user.username.trim()
        val initial = if (username.isNotEmpty()) {
            username.first().toString()
        } else {
            "?"  //hiển thị "?" nếu username hoàn toàn rỗng
        }
        nameInitial.text = initial

        // Hiển thị trạng thái đang nhập nếu người dùng đang nhập
        val typingIndicator = view.findViewById<View>(R.id.typing_indicator)
        typingIndicator.visibility = if (user.isTyping) View.VISIBLE else View.GONE

        addView(view)
        userViewMap[user.userId] = view
    }

    private fun updateUserView(user: UserInfo) {
        val view = userViewMap[user.userId] ?: return

        // Update typing indicator
        val typingIndicator = view.findViewById<View>(R.id.typing_indicator)
        typingIndicator.visibility = if (user.isTyping) View.VISIBLE else View.GONE
    }


    private fun showMoreUsersCount(count: Int) {
        val moreCountView = findViewWithTag<View>("more_count_view")

        if (moreCountView == null) {
            // Create and add the more count view
            val view = LayoutInflater.from(context).inflate(R.layout.item_more_users, this, false)
            val countText = view.findViewById<TextView>(R.id.more_users_count)
            countText.text = "+$count"
            view.tag = "more_count_view"
            addView(view)
        } else {
            // Update the existing more count view
            val countText = moreCountView.findViewById<TextView>(R.id.more_users_count)
            countText.text = "+$count"
        }
    }

    private fun hideMoreUsersCount() {
        val moreCountView = findViewWithTag<View>("more_count_view")
        if (moreCountView != null) {
            removeView(moreCountView)
        }
    }
}