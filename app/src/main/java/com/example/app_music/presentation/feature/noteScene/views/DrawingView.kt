package com.example.app_music.presentation.feature.noteScene.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.util.ArrayList
import android.graphics.Rect
import android.graphics.Region

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

    // Current drawing mode
    private var mCurrentMode = DrawMode.PAN

    // Variables for panning and zooming
    private var mPosX = 0f
    private var mPosY = 0f
    private var mScaleFactor = 1f
    private val mScaleDetector: ScaleGestureDetector
    private val mGestureDetector: GestureDetector

    // Paths management for undo/redo
    private val mDrawnPaths = mutableListOf<DrawAction>()
    private val mUndoActions = mutableListOf<UndoRedoAction>()
    private val mRedoActions = mutableListOf<UndoRedoAction>()
    private var mCurrentDrawAction: DrawAction? = null
    private var mTempEraserPath: Path? = null

    // Background image to use as canvas
    private var mBackgroundBitmap: Bitmap? = null
    private val mBackgroundPaint = Paint()

    // Document bounds
    private var mDocumentBounds = RectF()

    // Flag to track if drawing started outside document bounds
    private var mStartedOutsideBounds = false

    // Listener for draw completion
    interface OnDrawCompletedListener {
        fun onDrawCompleted()
    }

    private var mDrawCompletedListener: OnDrawCompletedListener? = null

    fun setOnDrawCompletedListener(listener: OnDrawCompletedListener) {
        mDrawCompletedListener = listener
        Log.d("DrawingView", "Draw completed listener set")
    }

    private fun notifyDrawCompleted() {
        try {
            Log.d("DrawingView", "Notifying draw completed")
            mDrawCompletedListener?.onDrawCompleted()
        } catch (e: Exception) {
            Log.e("DrawingView", "Error notifying draw completed", e)
        }
    }

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

        mBackgroundPaint.apply {
            isFilterBitmap = true
        }

        // Initialize scale detector
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())

        // Initialize gesture detector for panning
        mGestureDetector = GestureDetector(context, GestureListener())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Create a new bitmap and canvas when the view size changes
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)

        // Center the view position
        mPosX = 0f
        mPosY = 0f

        // Initialize document bounds based on view size
        mDocumentBounds.set(0f, 0f, w.toFloat(), h.toFloat())
    }

    fun setBackgroundImage(bitmap: Bitmap) {
        mBackgroundBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)

        // Update document bounds based on background image
        if (mBackgroundBitmap != null) {
            val left = (width - mBackgroundBitmap!!.width) / 2f
            val top = (height - mBackgroundBitmap!!.height) / 2f
            mDocumentBounds.set(
                left,
                top,
                left + mBackgroundBitmap!!.width,
                top + mBackgroundBitmap!!.height
            )
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Save current canvas state
        canvas.save()

        // Apply transformations for zoom and pan
        canvas.translate(mPosX, mPosY)
        canvas.scale(mScaleFactor, mScaleFactor)

        // Draw background image if exists
        mBackgroundBitmap?.let {
            // Calculate centered position
            val left = (width - it.width) / 2f
            val top = (height - it.height) / 2f
            canvas.drawBitmap(it, left, top, mBackgroundPaint)
        }

        // Draw the bitmap with all previous paths
        canvas.drawBitmap(mBitmap, 0f, 0f, null)

        // Draw the current path if in drawing mode
        if (mCurrentMode == DrawMode.DRAW) {
            canvas.drawPath(mPath, mPaint)
        } else if (mCurrentMode == DrawMode.ERASE && mTempEraserPath != null) {
            // Draw a visual feedback for eraser, but with semi-transparent color
            val eraserPaint = Paint(mPaint).apply {
                color = Color.LTGRAY
                alpha = 50 // Very transparent
                style = Paint.Style.STROKE
                strokeWidth = mPaint.strokeWidth * 1.5f
            }
            canvas.drawPath(mTempEraserPath!!, eraserPaint)
        }

        // Restore canvas to original state
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Always let the ScaleGestureDetector inspect the event first
        mScaleDetector.onTouchEvent(event)

        // If multitouch (zooming), prioritize that over other actions
        if (event.pointerCount > 1) {
            return true
        }

        // Handle touch based on current mode
        when (mCurrentMode) {
            DrawMode.PAN -> {
                // Let the gesture detector handle panning
                mGestureDetector.onTouchEvent(event)
                invalidate()
                return true
            }
            DrawMode.DRAW, DrawMode.ERASE -> {
                handleDrawingTouch(event)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleDrawingTouch(event: MotionEvent) {
        // Adjust coordinates for current zoom and pan
        val rawX = (event.x - mPosX) / mScaleFactor
        val rawY = (event.y - mPosY) / mScaleFactor

        // For ink mode, we want to directly draw on the canvas without transformation
        val drawX = rawX
        val drawY = rawY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Only start drawing if inside the document bounds
                if (isInsideDocumentBounds(drawX, drawY)) {
                    // Start a new stroke
                    mPath.reset()
                    mPath.moveTo(drawX, drawY)
                    mX = drawX
                    mY = drawY
                    mStartedOutsideBounds = false

                    if (mCurrentMode == DrawMode.ERASE) {
                        // Create a temporary eraser path for visual feedback
                        mTempEraserPath = Path()
                        mTempEraserPath?.moveTo(drawX, drawY)
                    } else {
                        // For normal drawing, create a new draw action
                        mCurrentDrawAction = DrawAction(Path(), Paint(mPaint), false)
                        mCurrentDrawAction?.path?.moveTo(drawX, drawY)
                    }
                } else {
                    mStartedOutsideBounds = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Skip if started outside document bounds
                if (mStartedOutsideBounds) {
                    return
                }

                // Calculate the distance moved
                val dx = Math.abs(drawX - mX)
                val dy = Math.abs(drawY - mY)

                // If the distance is significant enough
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    // Create a bezier curve from the previous point to the current point
                    mPath.quadTo(mX, mY, (drawX + mX) / 2, (drawY + mY) / 2)

                    if (mCurrentMode == DrawMode.ERASE) {
                        // Update temporary eraser path
                        mTempEraserPath?.quadTo(mX, mY, (drawX + mX) / 2, (drawY + mY) / 2)
                    } else {
                        // Update the drawing path
                        mCurrentDrawAction?.path?.quadTo(mX, mY, (drawX + mX) / 2, (drawY + mY) / 2)
                    }

                    // Update the last point
                    mX = drawX
                    mY = drawY
                }
            }

            MotionEvent.ACTION_UP -> {
                // Skip if started outside document bounds
                if (mStartedOutsideBounds) {
                    mStartedOutsideBounds = false
                    return
                }

                // Complete the path
                mPath.lineTo(mX, mY)

                if (mCurrentMode == DrawMode.ERASE) {
                    // Complete temporary eraser path
                    mTempEraserPath?.lineTo(mX, mY)
                    // Erase intersecting paths
                    val pathsRemoved = erasePaths()
                    // Save this action to undo stack only if paths were actually removed
                    if (pathsRemoved.isNotEmpty()) {
                        mUndoActions.add(EraseAction(ArrayList(pathsRemoved)))
                        mRedoActions.clear()
                        Log.d("DrawingView", "Added erase action with ${pathsRemoved.size} paths")
                        // Notify draw completion to save immediately
                        notifyDrawCompleted()
                    }
                    // Clear temporary eraser path
                    mTempEraserPath = null
                } else if (mCurrentMode == DrawMode.DRAW) {
                    // For drawing, save the path to the canvas only if a valid path exists
                    mCurrentDrawAction?.let {
                        if (!it.path.isEmpty) {
                            it.path.lineTo(mX, mY)
                            mDrawnPaths.add(it)
                            mCanvas.drawPath(it.path, it.paint)
                            mUndoActions.add(DrawPathAction(it))
                            mRedoActions.clear()
                            Log.d("DrawingView", "Added draw action")
                            // Notify draw completion to save immediately
                            notifyDrawCompleted()
                        }
                    }
                    mCurrentDrawAction = null
                }

                // Reset the path for the next drawing
                mPath.reset()

                // Log undo/redo status
                Log.d(
                    "DrawingView",
                    "Undo actions: ${mUndoActions.size}, Redo actions: ${mRedoActions.size}"
                )
            }
        }
    }

    private fun isInsideDocumentBounds(x: Float, y: Float): Boolean {
        // If we have a background image, check against its bounds
        return mDocumentBounds.contains(x, y)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (::mBitmap.isInitialized && !mBitmap.isRecycled) {
            mBitmap.recycle()
        }

        mBackgroundBitmap?.recycle()
        mBackgroundBitmap = null
    }

    private fun erasePaths(): List<DrawAction> {
        val pathsToRemove = mutableListOf<DrawAction>()

        // Create a region for the eraser path
        val eraserRegion = Region()
        val eraserPath = Path(mPath) // Clone the current path

        val rectF = RectF()
        eraserPath.computeBounds(rectF, true)
        val rect = Rect()
        rectF.roundOut(rect)

        eraserRegion.setPath(eraserPath, Region(rect))

        // Check each drawn path for intersection
        for (path in mDrawnPaths) {
            val pathRegion = Region()
            val pathRectF = RectF()
            path.path.computeBounds(pathRectF, true)
            val pathRect = Rect()
            pathRectF.roundOut(pathRect)

            // If the path bounds don't intersect the eraser bounds at all, skip further checks
            if (!Rect.intersects(rect, pathRect)) {
                continue
            }

            pathRegion.setPath(path.path, Region(pathRect))

            // If the regions intersect, mark path for removal
            if (!pathRegion.quickReject(eraserRegion) && pathRegion.op(eraserRegion, Region.Op.INTERSECT)) {
                pathsToRemove.add(path)
            }
        }

        // If any paths are to be removed
        if (pathsToRemove.isNotEmpty()) {
            // Remove the paths from drawn paths list
            mDrawnPaths.removeAll(pathsToRemove)

            // Redraw the canvas
            redrawPaths()
        }

        return pathsToRemove
    }

    /**
     * Set the color for drawing
     */
    fun setColor(color: Int) {
        mPaint.color = color
    }

    /**
     * Get the current drawing path for collaboration
     */
    fun getCurrentPath(): Path? {
        return if (mCurrentDrawAction != null) {
            mCurrentDrawAction?.path
        } else {
            null
        }
    }

    /**
     * Set the stroke width for drawing
     */
    fun setStrokeWidth(width: Float) {
        mPaint.strokeWidth = width
    }

    /**
     * Set the drawing mode
     */
    fun setMode(mode: DrawMode) {
        mCurrentMode = mode

        // In ink mode, we need to save the current state immediately
        if (mode == DrawMode.DRAW) {
            // Update document state if needed
        }
    }

    /**
     * Get the current drawing mode
     */
    fun getMode(): DrawMode {
        return mCurrentMode
    }

    /**
     * Clear the entire drawing
     */
    fun clearDrawing() {
        // Save current state before clearing
        val currentPaths = ArrayList(mDrawnPaths)
        if (currentPaths.isNotEmpty()) {
            mUndoActions.add(ClearAction(currentPaths))
            mRedoActions.clear()
            notifyDrawCompleted()
        }

        // Clear the canvas
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        mDrawnPaths.clear()
        invalidate()
    }

    /**
     * Undo the last drawing action
     */
    fun undo(): Boolean {
        Log.d("DrawingView", "Undo called with ${mUndoActions.size} actions")
        if (mUndoActions.isEmpty()) {
            return false
        }

        // Pop the last action from undo stack
        val lastIndex = mUndoActions.size - 1
        val undoAction = mUndoActions.removeAt(lastIndex)
        // Add it to redo stack
        mRedoActions.add(undoAction)

        // Process the undo action
        when (undoAction) {
            is DrawPathAction -> {
                // Remove the path
                Log.d("DrawingView", "Undoing draw action")
                mDrawnPaths.remove(undoAction.drawAction)
            }
            is EraseAction -> {
                // Add back the erased paths
                Log.d("DrawingView", "Undoing erase action with ${undoAction.erasedPaths.size} paths")
                mDrawnPaths.addAll(undoAction.erasedPaths)
            }
            is ClearAction -> {
                // Add back all cleared paths
                Log.d("DrawingView", "Undoing clear action with ${undoAction.clearedPaths.size} paths")
                mDrawnPaths.addAll(undoAction.clearedPaths)
            }
        }

        // Redraw everything from scratch
        redrawPaths()
        notifyDrawCompleted()
        return true
    }

    /**
     * Redo the last undone action
     */
    fun redo(): Boolean {
        Log.d("DrawingView", "Redo called with ${mRedoActions.size} actions")
        if (mRedoActions.isEmpty()) {
            return false
        }

        // Pop the last action from redo stack
        val lastIndex = mRedoActions.size - 1
        val redoAction = mRedoActions.removeAt(lastIndex)
        // Add it to undo stack
        mUndoActions.add(redoAction)

        // Process the redo action
        when (redoAction) {
            is DrawPathAction -> {
                // Add the path back
                Log.d("DrawingView", "Redoing draw action")
                mDrawnPaths.add(redoAction.drawAction)
            }
            is EraseAction -> {
                // Remove the paths again
                Log.d("DrawingView", "Redoing erase action with ${redoAction.erasedPaths.size} paths")
                mDrawnPaths.removeAll(redoAction.erasedPaths)
            }
            is ClearAction -> {
                // Clear all paths again
                Log.d("DrawingView", "Redoing clear action with ${redoAction.clearedPaths.size} paths")
                mDrawnPaths.removeAll(redoAction.clearedPaths)
            }
        }

        // Redraw everything from scratch
        redrawPaths()
        notifyDrawCompleted()
        return true
    }

    /**
     * Redraw all paths
     */
    private fun redrawPaths() {
        // Clear the canvas
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Redraw all paths
        for (path in mDrawnPaths) {
            try {
                mCanvas.drawPath(path.path, path.paint)
            } catch (e: Exception) {
                Log.e("DrawingView", "Error drawing path: ${e.message}")
            }
        }

        // Ensure UI is updated
        invalidate()
    }

    /**
     * Get the current drawing as a bitmap
     */
    fun getDrawingBitmap(): Bitmap {
        // Create a bitmap for just the drawing (without the background)
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Draw only the paths
        canvas.drawBitmap(mBitmap, 0f, 0f, null)

        return resultBitmap
    }

    /**
     * Get a combined bitmap with background image and drawing
     */
    fun getCombinedBitmap(): Bitmap {
        // Create a bitmap that includes both background and drawing
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Draw background if exists
        mBackgroundBitmap?.let {
            val left = (width - it.width) / 2f
            val top = (height - it.height) / 2f
            canvas.drawBitmap(it, left, top, mBackgroundPaint)
        }

        // Draw the paths
        canvas.drawBitmap(mBitmap, 0f, 0f, null)

        return resultBitmap
    }

    /**
     * Check if undo is available
     */
    fun canUndo(): Boolean = mUndoActions.isNotEmpty()

    /**
     * Check if redo is available
     */
    fun canRedo(): Boolean = mRedoActions.isNotEmpty()

    /**
     * Backward compatibility for the old API
     */
    @Deprecated("Use setMode(DrawMode.ERASE) instead", ReplaceWith("setMode(DrawMode.ERASE)"))
    fun enableEraser(enabled: Boolean) {
        if (enabled) {
            setMode(DrawMode.ERASE)
        } else {
            setMode(DrawMode.DRAW)
        }
    }

    /**
     * Scale listener for pinch-to-zoom
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor

            // Limit scale factor
            mScaleFactor = Math.max(0.5f, Math.min(mScaleFactor, 3.0f))

            invalidate()
            return true
        }
    }

    /**
     * Gesture listener for panning
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // Only pan if in PAN mode
            if (mCurrentMode == DrawMode.PAN) {
                mPosX -= distanceX
                mPosY -= distanceY
                invalidate()
                return true
            }
            return false
        }
    }

    /**
     * Drawing modes
     */
    enum class DrawMode {
        DRAW, ERASE, PAN
    }

    /**
     * Class to represent a drawing action with path and paint
     */
    data class DrawAction(
        val path: Path,
        val paint: Paint,
        val isEraser: Boolean
    )

    // Base class for undo/redo actions
    sealed class UndoRedoAction

    // Action for drawing a path
    data class DrawPathAction(val drawAction: DrawAction) : UndoRedoAction()

    // Action for erasing paths
    data class EraseAction(val erasedPaths: List<DrawAction>) : UndoRedoAction()

    // Action for clearing the canvas
    data class ClearAction(val clearedPaths: List<DrawAction>) : UndoRedoAction()
}