package com.example.app_music.presentation.feature.community.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app_music.R
import com.example.app_music.domain.model.Comment
import de.hdodenhof.circleimageview.CircleImageView

class CommentAdapter : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private var comments = listOf<Comment>()
    private var onCommentLikeListener: ((Comment) -> Unit)? = null
    private var onCommentReplyListener: ((Comment) -> Unit)? = null

    fun submitList(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    fun setOnCommentLikeListener(listener: (Comment) -> Unit) {
        onCommentLikeListener = listener
    }

    fun setOnCommentReplyListener(listener: (Comment) -> Unit) {
        onCommentReplyListener = listener
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
        private val ivImage: ImageView = itemView.findViewById(R.id.ivCommentImage)
        private val tvLike: TextView = itemView.findViewById(R.id.tvCommentLike)
        private val tvReply: TextView = itemView.findViewById(R.id.tvCommentReply)

        fun bind(comment: Comment) {
            tvUserName.text = comment.user.username
            tvTimeAgo.text = comment.getTimeAgo()
            tvContent.text = comment.content

            // Load user avatar
            if (!comment.user.avatarUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(comment.user.avatarUrl)
                    .placeholder(R.drawable.avatar)
                    .into(ivUserAvatar)
            }

            // Load comment image if present
            if (!comment.image.isNullOrEmpty()) {
                ivImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(comment.image)
                    .into(ivImage)
            } else {
                ivImage.visibility = View.GONE
            }

            // Set click listeners
            tvLike.setOnClickListener {
                onCommentLikeListener?.invoke(comment)
            }

            tvReply.setOnClickListener {
                onCommentReplyListener?.invoke(comment)
            }
        }
    }
}