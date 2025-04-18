package com.example.app_music.presentation.noteScene.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Paint object for drawing paths
    private var mPaint: Paint = Paint()
    // Bitmap for the canvas
    private lateinit var mBitmap: Bitmap
    // Canvas to draw on
    private lateinit var mCanvas: Canvas
    // Path that is being drawn
    private val mPath = Path()
    // Coordinates of the last touch point
    private var mX = 0f
    private var mY = 0f
    // Threshold for considering touch as moved
    private val TOUCH_TOLERANCE = 4f
    // Flag for eraser mode
    private var mIsEraserActive = false

    init {
        // Initialize the paint with default values
        mPaint.apply {
            isAntiAlias = true
            isDither = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 5f
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Create a new bitmap and canvas when the view size changes
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw the bitmap
        canvas.drawBitmap(mBitmap, 0f, 0f, null)
        // Draw the current path
        canvas.drawPath(mPath, mPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Start a new path
                touchStart(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                // Continue the path
                touchMove(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                // End the path and draw it to the canvas
                touchUp()
                invalidate()
            }
        }
        return true
    }

    private fun touchStart(x: Float, y: Float) {
        // Clear any existing path
        mPath.reset()
        // Move to the touch point
        mPath.moveTo(x, y)
        // Save the current coordinates
        mX = x
        mY = y
    }

    private fun touchMove(x: Float, y: Float) {
        // Calculate the distance moved
        val dx = Math.abs(x - mX)
        val dy = Math.abs(y - mY)

        // If the distance is significant enough
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            // Create a bezier curve from the previous point to the current point
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            // Update the last point
            mX = x
            mY = y
        }
    }

    private fun touchUp() {
        // Complete the path
        mPath.lineTo(mX, mY)
        // Draw the path to the canvas
        mCanvas.drawPath(mPath, mPaint)
        // Reset the path for the next drawing
        mPath.reset()
    }

    /**
     * Set the color for drawing
     */
    fun setColor(color: Int) {
        mPaint.color = color
        if (mIsEraserActive) {
            // If eraser was active, reset to normal drawing mode
            mIsEraserActive = false
            mPaint.xfermode = null
        }
    }

    /**
     * Set the stroke width for drawing
     */
    fun setStrokeWidth(width: Float) {
        mPaint.strokeWidth = width
    }

    /**
     * Enable or disable eraser mode
     */
    fun enableEraser(isEnabled: Boolean) {
        mIsEraserActive = isEnabled
        if (isEnabled) {
            // Use CLEAR mode for the eraser (it removes pixels)
            mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            // Use normal drawing mode
            mPaint.xfermode = null
        }
    }

    /**
     * Clear the entire drawing
     */
    fun clearDrawing() {
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }
    
    /**
     * Get the current drawing as a bitmap
     */
    fun getDrawingBitmap(): Bitmap {
        return mBitmap.config?.let { config ->
            mBitmap.copy(config, true)
        } ?: mBitmap.copy(Bitmap.Config.ARGB_8888, true)
    }
}