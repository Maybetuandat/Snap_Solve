package com.example.app_music.presentation.feature.noteScene.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.app_music.R
import com.example.app_music.data.collaboration.CollaborationManager.UserInfo

class UserPresenceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val maxUsersVisible = 3
    private var currentUserId = ""

    fun setCurrentUserId(userId: String) {
        this.currentUserId = userId
    }

    fun updateActiveUsers(users: List<UserInfo>) {
        // XÓA HẾT TẤT CẢ VIEW CŨ
        removeAllViews()

        // HIỂN THỊ TẤT CẢ người dùng đang online (bao gồm cả user hiện tại)
        val activeUsers = users.filter { !it.isOffline }

        if (activeUsers.isEmpty()) {
            return
        }

        // Sắp xếp để user hiện tại luôn hiển thị đầu tiên
        val sortedUsers = activeUsers.sortedBy {
            if (it.userId == currentUserId) 0 else 1
        }

        // ADD LẠI TẤT CẢ USER TỪ ĐẦU
        sortedUsers.take(maxUsersVisible).forEach { user ->
            addUserView(user)
        }

        // Hiển thị số người dùng thêm nếu cần
        if (activeUsers.size > maxUsersVisible) {
            showMoreUsersCount(activeUsers.size - maxUsersVisible)
        }
    }

    private fun addUserView(user: UserInfo) {
        val view = LayoutInflater.from(context).inflate(R.layout.item_active_user, this, false)
        val indicator = view.findViewById<View>(R.id.user_color_indicator)
        val nameInitial = view.findViewById<TextView>(R.id.user_initial)
        val meLabel = view.findViewById<TextView>(R.id.me_label)

        // Đặt chữ cái đầu của tên người dùng, xử lý tên rỗng
        val username = user.username.trim()
        val initial = if (username.isNotEmpty()) {
            username.first().toString()
        } else {
            "?"  //hiển thị "?" nếu username hoàn toàn rỗng
        }
        nameInitial.text = initial

        // Nếu là user hiện tại, thêm dấu hiệu đặc biệt
        if (user.userId == currentUserId) {
            // Sử dụng drawable với viền vàng cho user hiện tại
            indicator.background = ContextCompat.getDrawable(context, R.drawable.current_user_indicator)
            // Tạo GradientDrawable để set màu nền
            val drawable = indicator.background as? android.graphics.drawable.GradientDrawable
            drawable?.setColor(user.color)

            // Hiển thị label "You"
            meLabel?.visibility = View.VISIBLE
            meLabel?.text = "You"
        } else {
            // User khác - chỉ dùng hình tròn bình thường
            indicator.background = ContextCompat.getDrawable(context, R.drawable.user_circle_background)
            val drawable = indicator.background as? android.graphics.drawable.GradientDrawable
            drawable?.setColor(user.color)

            // Ẩn label "You"
            meLabel?.visibility = View.GONE
        }

        // Hiển thị trạng thái đang nhập nếu người dùng đang nhập
        val typingIndicator = view.findViewById<View>(R.id.typing_indicator)
        typingIndicator?.visibility = if (user.isTyping) View.VISIBLE else View.GONE

        // ADD VIEW VÀO CONTAINER
        addView(view)
    }

    private fun showMoreUsersCount(count: Int) {
        // Create and add the more count view
        val view = LayoutInflater.from(context).inflate(R.layout.item_more_users, this, false)
        val countText = view.findViewById<TextView>(R.id.more_users_count)
        countText.text = "+$count"
        addView(view)
    }
}