package com.example.app_music.presentation.feature.noteScene.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.app_music.R
import com.example.app_music.data.collaboration.CollaborationManager
import com.example.app_music.data.collaboration.CollaborationManager.UserInfo

/**
 * View showing active users in a collaborative editing session
 */
class UserPresenceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val userViewMap = mutableMapOf<String, View>()
    private val maxUsersVisible = 3
    private var currentUserId: String = "" // Store the current user ID

    init {
        orientation = HORIZONTAL
    }

    /**
     * Set the current user ID for stale user detection
     */
    fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    /**
     * Update the list of active users
     */
    fun updateActiveUsers(users: List<UserInfo>) {
        // Filter out stale users
        val activeUsers = removeStaleUsers(users)

        // First, remove any users that are no longer active
        val currentUserIds = activeUsers.map { it.userId }
        val toRemove = userViewMap.keys.filter { it !in currentUserIds }

        toRemove.forEach { userId ->
            val view = userViewMap[userId] ?: return@forEach
            removeView(view)
            userViewMap.remove(userId)
        }

        // Now add or update current users
        activeUsers.take(maxUsersVisible).forEach { user ->
            if (user.userId in userViewMap) {
                // Update existing user
                updateUserView(user)
            } else {
                // Add new user
                addUserView(user)
            }
        }

        // Show additional users count if needed
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

        // Set user's color
        indicator.setBackgroundColor(user.color)

        // Set user's initial
        val initial = user.username.firstOrNull()?.toString() ?: "?"
        nameInitial.text = initial

        // Show typing indicator if user is typing
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

    private fun removeStaleUsers(users: List<UserInfo>): List<UserInfo> {
        val currentTime = System.currentTimeMillis()
        val timeoutThreshold = 60000 // 1 minute timeout

        return users.filter { user ->
            // Keep users with recent activity or if they're the current user
            val isCurrentUser = user.userId == currentUserId
            val isRecentlyActive = currentTime - user.lastActive < timeoutThreshold

            isCurrentUser || isRecentlyActive
        }
    }

    private fun showMoreUsersCount(count: Int) {
        // Check if the more count view already exists
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