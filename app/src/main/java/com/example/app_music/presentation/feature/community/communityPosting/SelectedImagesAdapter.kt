package com.example.app_music.presentation.feature.community.communityPosting

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app_music.R

class SelectedImagesAdapter : RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder>() {

    private val imageUris = mutableListOf<Uri>()
    private var onImageRemoveListener: ((Int) -> Unit)? = null

    fun setOnImageRemoveListener(listener: (Int) -> Unit) {
        onImageRemoveListener = listener
    }

    fun submitList(images: List<Uri>) {
        imageUris.clear()
        imageUris.addAll(images)
        notifyDataSetChanged()
    }

    fun addImage(uri: Uri) {
        imageUris.add(uri)
        notifyItemInserted(imageUris.size - 1)
    }

    fun removeImage(position: Int) {
        if (position in 0 until imageUris.size) {
            imageUris.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, imageUris.size - position)
        }
    }

    fun getImages(): List<Uri> = imageUris.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUris[position], position)
    }

    override fun getItemCount(): Int = imageUris.size

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(uri: Uri, position: Int) {
            // Tải ảnh vào ImageView
            Glide.with(itemView.context)
                .load(uri)
                .centerCrop()
                .into(ivImage)

            // Thiết lập sự kiện click để xóa ảnh
            btnRemove.setOnClickListener {
                onImageRemoveListener?.invoke(position)
            }
        }
    }
}