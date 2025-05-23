package com.example.app_music.presentation.feature.notification

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app_music.R
import com.example.app_music.domain.model.Notification
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class NotificationAdapter(
    private val context: Context,
    private val onNotificationClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvNotificationContent)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition // Thay thế bindingAdapterPosition bằng adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNotificationClick(getItem(position))
                }
            }
        }

        fun bind(notification: Notification) {
            tvTitle.text = notification.title
            tvContent.text = notification.content
            tvTime.text = getFormattedTime(notification.notiDate)

            // Set unread indicator visibility
            unreadIndicator.visibility = if (!notification.isRead) View.VISIBLE else View.GONE

            // You can customize the icon based on notification type if needed
            when {
                notification.title.contains("Premium", ignoreCase = true) -> {
                    ivIcon.setImageResource(R.drawable.ic_premium)
                }
                notification.title.contains("thanh toán", ignoreCase = true) -> {
                    ivIcon.setImageResource(R.drawable.ic_payment)
                }
                else -> {
                    ivIcon.setImageResource(R.drawable.ic_notification)
                }
            }
        }

        private fun getFormattedTime(dateString: String): String {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val date = LocalDate.parse(dateString, formatter)
                val now = LocalDate.now()
                val daysBetween = ChronoUnit.DAYS.between(date, now)

                return when {
                    daysBetween == 0L -> "Hôm nay"
                    daysBetween == 1L -> "Hôm qua"
                    daysBetween < 7L -> "$daysBetween ngày trước"
                    daysBetween < 30L -> "${daysBetween / 7} tuần trước"
                    daysBetween < 365L -> "${daysBetween / 30} tháng trước"
                    else -> "${daysBetween / 365} năm trước"
                }
            } catch (e: Exception) {
                return dateString
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}