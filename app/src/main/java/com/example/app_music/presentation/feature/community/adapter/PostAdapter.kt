package com.example.app_music.presentation.feature.community.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app_music.R
import com.example.app_music.domain.model.Post
import com.example.app_music.domain.utils.UrlUtils
import java.time.LocalDate
import java.time.Period

class PostAdapter : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var posts = listOf<Post>()
    private var onPostClickListener: ((Post) -> Unit)? = null
    private var onLikeClickListener: ((Post) -> Unit)? = null
    private var onCommentClickListener: ((Post) -> Unit)? = null
    private var currentUserId: Long = 0

    fun setCurrentUserId(userId: Long) {
        currentUserId = userId
        notifyDataSetChanged() // Làm mới các view để cập nhật biểu tượng thích
    }

    fun submitList(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    fun setOnPostClickListener(listener: (Post) -> Unit) {
        onPostClickListener = listener
    }

    fun setOnLikeClickListener(listener: (Post) -> Unit) {
        onLikeClickListener = listener
    }

    fun setOnCommentClickListener(listener: (Post) -> Unit) {
        onCommentClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivUserAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        private val tvPostTitle: TextView = itemView.findViewById(R.id.tvPostTitle)
        private val tvPostContent: TextView = itemView.findViewById(R.id.tvPostContent)
        private val ivPostImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val tvTag1: TextView = itemView.findViewById(R.id.tvTag1)
        private val tvTag2: TextView = itemView.findViewById(R.id.tvTag2)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        private val btnLike: LinearLayout = itemView.findViewById(R.id.btnLike)
        private val btnComment: LinearLayout = itemView.findViewById(R.id.btnComment)
        private val ivLike: ImageView = itemView.findViewById(R.id.ivLike)

        // Thêm biến để theo dõi trạng thái thích cục bộ
        private var isLikedLocally = false

        fun bind(post: Post) {
            tvUserName.text = post.user.username
            tvTimeAgo.text = getTimeAgo(post.createDate)
            tvPostTitle.text = post.title
            tvPostContent.text = post.content

            // Kiểm tra xem người dùng hiện tại đã thích bài viết này chưa
            val hasUserLiked = post.react.any { it.user.id == currentUserId }

            // Khởi tạo trạng thái thích cục bộ với giá trị từ server
            isLikedLocally = hasUserLiked

            // Cập nhật UI dựa trên trạng thái thích cục bộ
            updateLikeUI(isLikedLocally)



            val userAvaterUrl = UrlUtils.getAbsoluteUrl(post.user.avatarUrl)
            // Hiển thị ảnh người dùng nếu có
            if (!post.user.avatarUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(userAvaterUrl)
                    .placeholder(R.drawable.avatar)
                    .into(ivUserAvatar)
            }


            // Hiển thị ảnh bài viết nếu có
            val imageUrl = UrlUtils.getAbsoluteUrl(post.image)
            if (imageUrl.isNotEmpty()) {
                ivPostImage.visibility = View.VISIBLE

                // Sử dụng thumbnail (image chính) từ bài viết
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.lorem)
                    .into(ivPostImage)
            } else {
                // Nếu không có ảnh chính, kiểm tra xem có ảnh phụ không
                if (!post.additionalImages.isNullOrEmpty()) {
                    ivPostImage.visibility = View.VISIBLE
                    // Hiển thị ảnh đầu tiên từ additionalImages nếu không có thumbnail
                    val additionalImageUrl = UrlUtils.getAbsoluteUrl(post.additionalImages.first())
                    Glide.with(itemView.context)
                        .load(additionalImageUrl)
                        .placeholder(R.drawable.lorem)
                        .into(ivPostImage)
                } else {
                    // Ẩn ImageView nếu không có ảnh nào
                    ivPostImage.visibility = View.GONE
                }
            }


            // Hiển thị các chủ đề
            if (post.topics.isNotEmpty()) {
                tvTag1.visibility = View.VISIBLE
                tvTag1.text = post.topics[0].name

                if (post.topics.size > 1) {
                    tvTag2.visibility = View.VISIBLE
                    tvTag2.text = post.topics[1].name
                } else {
                    tvTag2.visibility = View.GONE
                }
            } else {
                tvTag1.visibility = View.GONE
                tvTag2.visibility = View.GONE
            }

            // Hiển thị số lượng like và comment
            tvLikeCount.text = post.reactCount.toString()
            tvCommentCount.text = post.commentCount.toString()

            // Set up click listeners
            itemView.setOnClickListener {
                onPostClickListener?.invoke(post)
            }

            btnLike.setOnClickListener {
                // Đảo ngược trạng thái thích cục bộ
                isLikedLocally = !isLikedLocally

                // Cập nhật UI
                updateLikeUI(isLikedLocally)

                // Gọi listener để xử lý thao tác thích/bỏ thích thực tế trên máy chủ
                onLikeClickListener?.invoke(post)
            }

            btnComment.setOnClickListener {
                onCommentClickListener?.invoke(post)
            }
        }
        // Thêm phương thức này để cập nhật UI dựa trên trạng thái thích
        private fun updateLikeUI(isLiked: Boolean) {
            if (isLiked) {
                ivLike.setImageResource(R.drawable.ic_liked_red) // Biểu tượng trái tim đã thích

            } else {
                ivLike.setImageResource(R.drawable.ic_liked) // Biểu tượng trái tim chưa thích
            }
        }

        private fun getTimeAgo(date: LocalDate): String {
            val now = LocalDate.now()
            val period = Period.between(date, now)

            return when {
                period.years > 0 -> "${period.years} năm trước"
                period.months > 0 -> "${period.months} tháng trước"
                period.days > 0 -> "${period.days} ngày trước"
                else -> "Hôm nay"
            }
        }
    }
}