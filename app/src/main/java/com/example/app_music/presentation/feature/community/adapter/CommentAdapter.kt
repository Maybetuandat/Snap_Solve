package com.example.app_music.presentation.feature.community.adapter

import android.app.Dialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app_music.R
import com.example.app_music.domain.model.Comment
import com.example.app_music.domain.utils.UrlUtils
import de.hdodenhof.circleimageview.CircleImageView

class CommentAdapter : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private var comments = listOf<Comment>()
    private var onCommentReplyListener: ((Comment) -> Unit)? = null
    private var onViewRepliesListener: ((Comment) -> Unit)? = null

    fun submitList(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    fun setOnCommentReplyListener(listener: (Comment) -> Unit) {
        onCommentReplyListener = listener
    }

    fun setOnViewRepliesListener(listener: (Comment) -> Unit) {
        onViewRepliesListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivUserAvatar: CircleImageView = itemView.findViewById(R.id.ivCommentUserAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvCommentUserName)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvCommentTimeAgo)
        private val tvContent: TextView = itemView.findViewById(R.id.tvCommentContent)
        private val rvCommentImages: RecyclerView = itemView.findViewById(R.id.rvCommentImages)
        private val tvCommentReply: TextView = itemView.findViewById(R.id.tvCommentReply)

        private val commentImagesAdapter = CommentImagesAdapter()


        fun bind(comment: Comment) {
            Log.d("CommentAdapter", "Binding comment:")
            Log.d("CommentAdapter", "  ID: ${comment.id}")
            Log.d("CommentAdapter", "  Content: ${comment.content}")
            Log.d("CommentAdapter", "  ReplyCount: ${comment.replyCount}")
            Log.d("CommentAdapter", "  User: ${comment.user.username}")
            tvUserName.text = comment.user.username
            tvTimeAgo.text = comment.getTimeAgo()
            tvContent.text = comment.content

            // Load user avatar
            val avatarUrl = UrlUtils.getAbsoluteUrl(comment.user.avatarUrl)
            if (avatarUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.avatar)
                    .into(ivUserAvatar)
            }

            // Hiển thị ảnh của comment nếu có
            if (comment.images.isNotEmpty()) {
                rvCommentImages.visibility = View.VISIBLE
                rvCommentImages.layoutManager = GridLayoutManager(itemView.context, 2)
                rvCommentImages.adapter = commentImagesAdapter
                commentImagesAdapter.submitList(comment.images)
            } else {
                rvCommentImages.visibility = View.GONE
            }

            // Xử lý hiển thị nút trả lời/xem trả lời
            if (comment.replyCount > 0) {
                tvCommentReply.text = "Xem ${comment.replyCount} trả lời"
                tvCommentReply.setOnClickListener {
                    onViewRepliesListener?.invoke(comment)
                }
            } else {
                tvCommentReply.text = "Trả lời"
                tvCommentReply.setOnClickListener {
                    onCommentReplyListener?.invoke(comment)
                }
            }
            commentImagesAdapter.setOnImageClickListener { imageUrl, position ->
                showFullscreenImage(imageUrl)
            }
        }

        private fun showFullscreenImage(imageUrl: String) {
            // Tạo một dialog hiển thị ảnh toàn màn hình
            val dialog = Dialog(itemView.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            val imageView = ImageView(itemView.context)

            // Thiết lập thuộc tính cho ImageView
            imageView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER

            // Tải ảnh vào ImageView
            Glide.with(itemView.context)
                .load(imageUrl)
                .into(imageView)

            // Thiết lập sự kiện click để đóng dialog
            imageView.setOnClickListener {
                dialog.dismiss()
            }

            // Hiển thị dialog
            dialog.setContentView(imageView)
            dialog.show()
        }
    }
}