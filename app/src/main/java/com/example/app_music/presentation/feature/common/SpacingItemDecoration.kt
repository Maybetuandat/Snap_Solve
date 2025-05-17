package com.example.app_music.presentation.feature.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        outRect.left = spacing
        outRect.right = spacing
        outRect.bottom = spacing
        
        // Add top spacing only for the first item to avoid double space between items
        if (position == 0) {
            outRect.top = spacing
        }
    }
}