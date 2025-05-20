package com.example.app_music.presentation.feature.searchhistory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app_music.R
import com.example.app_music.domain.model.SearchHistory
import com.example.app_music.domain.utils.UrlUtils

class SearchHistoryAdapter(private val onItemClick: (SearchHistory) -> Unit) :
    ListAdapter<SearchHistory, SearchHistoryAdapter.ViewHolder>(SearchHistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewHistory)
        private val textQuestion: TextView = itemView.findViewById(R.id.textViewQuestion)
        private val textDate: TextView = itemView.findViewById(R.id.textViewDate)
//        private val textType: TextView = itemView.findViewById(R.id.textViewType)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: SearchHistory) {
            textQuestion.text = item.question
            textDate.text = item.createDate

            // Set type based on whether it's an image or text search
            if (item.image.isNullOrEmpty()) {
//                textType.text = "Text Search"
                // Show default search icon
                imageView.setImageResource(R.drawable.ic_search_history)
            } else {
//                textType.text = "Image Search"
                // Load image from URL if available
                Glide.with(itemView.context)
                    .load(UrlUtils.getAbsoluteUrl(item.image))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.ic_search_history)
                    .centerCrop()
                    .into(imageView)
            }
        }
    }

    private class SearchHistoryDiffCallback : DiffUtil.ItemCallback<SearchHistory>() {
        override fun areItemsTheSame(oldItem: SearchHistory, newItem: SearchHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SearchHistory, newItem: SearchHistory): Boolean {
            return oldItem == newItem
        }
    }
}