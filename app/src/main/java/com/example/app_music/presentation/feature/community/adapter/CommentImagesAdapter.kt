package com.example.app_music.presentation.feature.community.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app_music.R
import com.example.app_music.domain.utils.UrlUtils

class CommentImagesAdapter : RecyclerView.Adapter<CommentImagesAdapter.ImageViewHolder>() {

    private var imageUrls = listOf<String>()
    private var onImageClickListener: ((String, Int) -> Unit)? = null

    fun submitList(images: List<String>) {
        imageUrls = images
        notifyDataSetChanged()
    }

    fun setOnImageClickListener(listener: (String, Int) -> Unit) {
        onImageClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUrls[position], position)
    }

    override fun getItemCount(): Int = imageUrls.size

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCommentImage: ImageView = itemView.findViewById(R.id.ivCommentImage)

        fun bind(imageUrl: String, position: Int) {
            // Chuyển đổi thành URL tuyệt đối
            val absoluteUrl = UrlUtils.getAbsoluteUrl(imageUrl)

            // Tải ảnh vào ImageView
            Glide.with(itemView.context)
                .load(absoluteUrl)
                .centerCrop()
                .into(ivCommentImage)

            // Thiết lập sự kiện click
            itemView.setOnClickListener {
                onImageClickListener?.invoke(absoluteUrl, position)
            }
        }
    }
}