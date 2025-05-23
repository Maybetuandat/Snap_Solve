package com.example.app_music.utils


import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.drawable.updateBounds
import com.google.android.material.badge.BadgeDrawable
import kotlin.math.roundToInt

/**
 * Extension function to convert a BadgeDrawable to a regular Drawable that can be added to a view's overlay
 */
fun BadgeDrawable.toDrawable(context: Context): Drawable {
    return object : Drawable() {
        private val rect = Rect()

        override fun draw(canvas: android.graphics.Canvas) {
            updateBadgeBounds(rect)
            this@toDrawable.draw(canvas)
        }

        override fun setAlpha(alpha: Int) {
            this@toDrawable.alpha = alpha
        }

        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            // Not supported by BadgeDrawable
        }

        override fun getOpacity(): Int {
            return android.graphics.PixelFormat.TRANSLUCENT
        }

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            updateBadgeBounds(bounds)
        }

        private fun updateBadgeBounds(bounds: Rect) {
            val badgeDiameter = this@toDrawable.intrinsicHeight
            val badgeOffsetX = this@toDrawable.horizontalOffset
            val badgeOffsetY = this@toDrawable.verticalOffset

            // Apply the horizontal and vertical offsets
            val centerX = bounds.right - (badgeDiameter / 2f) + badgeOffsetX
            val centerY = bounds.top + (badgeDiameter / 2f) + badgeOffsetY

            val halfSize = badgeDiameter / 2f
            this@toDrawable.setBounds(
                (centerX - halfSize).roundToInt(),
                (centerY - halfSize).roundToInt(),
                (centerX + halfSize).roundToInt(),
                (centerY + halfSize).roundToInt()
            )
        }
    }.apply {
        updateBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }
}

/**
 * Extension function to attach a BadgeDrawable to a View
 */
fun View.attachBadge(badgeDrawable: BadgeDrawable) {
    badgeDrawable.apply {
        val badgeWidth = intrinsicWidth
        val badgeHeight = intrinsicHeight

        // Default position - top right corner
        setBounds(
            width - badgeWidth,
            0,
            width,
            badgeHeight
        )

        // Add badge to the view's overlay
        overlay.add(toDrawable(context))
    }
}