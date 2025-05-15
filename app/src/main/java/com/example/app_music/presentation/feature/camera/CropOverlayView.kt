
package com.example.app_music.presentation.feature.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val rectPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000") // Semi-transparent black
        style = Paint.Style.FILL
    }

    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = 5f
    }

    private val cropRect = RectF(100f, 100f, 300f, 200f)
    private val handleRadius = 20f

    private var activeHandle: Int = NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var cropRectInitialized = false

    companion object {
        private const val NONE = 0
        private const val TOP_LEFT = 1
        private const val TOP_RIGHT = 2
        private const val BOTTOM_LEFT = 3
        private const val BOTTOM_RIGHT = 4
        private const val CENTER = 5
        private const val TAG = "CropOverlayView"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the overlay (semi-transparent background)
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, overlayPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, overlayPaint)

        // Draw the crop rectangle
        canvas.drawRect(cropRect, rectPaint)

        // Draw the handles
        canvas.drawCircle(cropRect.left, cropRect.top, handleRadius, handlePaint)
        canvas.drawCircle(cropRect.right, cropRect.top, handleRadius, handlePaint)
        canvas.drawCircle(cropRect.left, cropRect.bottom, handleRadius, handlePaint)
        canvas.drawCircle(cropRect.right, cropRect.bottom, handleRadius, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeHandle = getActiveHandle(x, y)
                lastTouchX = x
                lastTouchY = y
                return activeHandle != NONE
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeHandle != NONE) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY

                    when (activeHandle) {
                        TOP_LEFT -> {
                            cropRect.left = (cropRect.left + dx).coerceIn(0f, cropRect.right - 50f)
                            cropRect.top = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - 50f)
                        }
                        TOP_RIGHT -> {
                            cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + 50f, width.toFloat())
                            cropRect.top = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - 50f)
                        }
                        BOTTOM_LEFT -> {
                            cropRect.left = (cropRect.left + dx).coerceIn(0f, cropRect.right - 50f)
                            cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + 50f, height.toFloat())
                        }
                        BOTTOM_RIGHT -> {
                            cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + 50f, width.toFloat())
                            cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + 50f, height.toFloat())
                        }
                        CENTER -> {
                            val widthBefore = cropRect.width()
                            val heightBefore = cropRect.height()

                            cropRect.left = (cropRect.left + dx).coerceIn(0f, width - widthBefore)
                            cropRect.top = (cropRect.top + dy).coerceIn(0f, height - heightBefore)
                            cropRect.right = cropRect.left + widthBefore
                            cropRect.bottom = cropRect.top + heightBefore
                        }
                    }

                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = NONE
            }
        }

        return super.onTouchEvent(event)
    }

    private fun getActiveHandle(x: Float, y: Float): Int {
        // Check if touch is on any of the handles
        if (isNearPoint(x, y, cropRect.left, cropRect.top)) return TOP_LEFT
        if (isNearPoint(x, y, cropRect.right, cropRect.top)) return TOP_RIGHT
        if (isNearPoint(x, y, cropRect.left, cropRect.bottom)) return BOTTOM_LEFT
        if (isNearPoint(x, y, cropRect.right, cropRect.bottom)) return BOTTOM_RIGHT

        // Check if touch is inside the crop rect
        if (cropRect.contains(x, y)) return CENTER

        return NONE
    }

    private fun isNearPoint(touchX: Float, touchY: Float, pointX: Float, pointY: Float): Boolean {
        val dx = touchX - pointX
        val dy = touchY - pointY
        return (dx * dx + dy * dy) <= (handleRadius * handleRadius * 2)
    }

    // Method to get the selected rectangle (called from the activity)
    fun getSelectedRect(): RectF? {
        return if (cropRect.width() > 50 && cropRect.height() > 50) {
            RectF(cropRect)
        } else {
            null
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // If we don't have bitmap dimensions yet, use default initialization
        if (!cropRectInitialized) {
            initializeCropRect(w, h)
            cropRectInitialized = true
        }
    }

    // For backward compatibility
    fun initializeCropRect(width: Int, height: Int) {
        val cropWidth = width * 0.8f
        val cropHeight = height * 0.4f

        cropRect.left = (width - cropWidth) / 2
        cropRect.top = (height - cropHeight) / 2
        cropRect.right = cropRect.left + cropWidth
        cropRect.bottom = cropRect.top + cropHeight

        invalidate()
    }

    // Initialize the crop rectangle for a specific bitmap with view dimensions
    fun initializeCropRectForBitmap(bitmapWidth: Int, bitmapHeight: Int, viewWidth: Int, viewHeight: Int) {
        Log.d(TAG, "Initializing crop rect for bitmap: ${bitmapWidth}x${bitmapHeight}, view: ${viewWidth}x${viewHeight}")

        // Calculate the scale factor between the bitmap and the view
        val bitmapAspect = bitmapWidth.toFloat() / bitmapHeight
        val viewAspect = viewWidth.toFloat() / viewHeight

        // Calculate visible image area within the view
        var visibleWidth = viewWidth.toFloat()
        var visibleHeight = viewHeight.toFloat()
        var offsetX = 0f
        var offsetY = 0f

        if (viewAspect > bitmapAspect) {
            // View is wider than the bitmap, image is stretched vertically
            visibleWidth = viewHeight * bitmapAspect
            offsetX = (viewWidth - visibleWidth) / 2
        } else {
            // View is taller than the bitmap, image is stretched horizontally
            visibleHeight = viewWidth / bitmapAspect
            offsetY = (viewHeight - visibleHeight) / 2
        }

        // Create a crop rect that's 80% of the visible image size and centered
        val cropWidth = visibleWidth * 0.8f
        val cropHeight = visibleHeight * 0.8f

        cropRect.left = offsetX + (visibleWidth - cropWidth) / 2
        cropRect.top = offsetY + (visibleHeight - cropHeight) / 2
        cropRect.right = cropRect.left + cropWidth
        cropRect.bottom = cropRect.top + cropHeight

        cropRectInitialized = true
        invalidate()

        Log.d(TAG, "Initialized crop rect: $cropRect")
    }
}