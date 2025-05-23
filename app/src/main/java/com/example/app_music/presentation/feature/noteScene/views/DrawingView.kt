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
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class DrawMode {
        DRAW,
        ERASE,
        PAN,
        SELECT
    }

    companion object {
        private const val TAG = "DrawingView"
        private const val DEFAULT_STROKE_WIDTH = 5f
        private const val DEFAULT_COLOR = Color.BLACK
        private const val TOUCH_TOLERANCE = 4f
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 5.0f
        private const val STROKE_SELECTION_TOLERANCE = 20f
    }

    private val mPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = DEFAULT_COLOR
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = DEFAULT_STROKE_WIDTH
    }

    private lateinit var mBitmap: Bitmap
    private lateinit var mCanvas: Canvas

    // Các biến lưu trạng thái vẽ
    private var mCurrentMode = DrawMode.DRAW
    private var mSelectedStrokeId: String? = null

    // Các biến cho zooming và panning
    private var mPosX = 0f
    private var mPosY = 0f
    private var mScaleFactor = 1f
    private val mScaleDetector: ScaleGestureDetector
    private val mGestureDetector: GestureDetector
    private var mBackgroundRect = RectF() // To track background positioning
    private var mBackgroundWidth = 0
    private var mBackgroundHeight = 0

    // Background image
    private var mBackgroundBitmap: Bitmap? = null
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mLastEditTime: Long = 0L

    private val mBackgroundPaint = Paint().apply {
        isFilterBitmap = true
    }

    private val mEraserHintPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = 2f
        alpha = 128
    }

    private var mCanvasWidth = 0
    private var mCanvasHeight = 0

    // Listeners
    interface OnDrawCompletedListener {
        fun onDrawCompleted()
    }

    private var mDrawCompletedListener: OnDrawCompletedListener? = null
    private var mStrokeSelectedListener: ((String) -> Unit)? = null

    // Danh sách các nét vẽ
    private val mStrokes = mutableListOf<Stroke>()
    private var mCurrentStroke: Stroke? = null

    private val mDeletedStrokes = mutableListOf<Stroke>()
    private var mForceSave = false

    data class StrokePoint(
        // Tọa độ chuẩn hóa (0.0 - 1.0) so với kích thước canvas
        @SerializedName("x") val x: Float = 0f,  // tọa độ chuẩn hóa thay vì pixel
        @SerializedName("y") val y: Float = 0f,  // tọa độ chuẩn hóa thay vì pixel
        @SerializedName("type") val type: Int = MotionEvent.ACTION_MOVE // ACTION_DOWN, ACTION_MOVE, ACTION_UP
    ) {
        // Tính toán tọa độ pixel dựa trên kích thước canvas hiện tại
        fun getPixelX(canvasWidth: Int): Float = x * canvasWidth
        fun getPixelY(canvasHeight: Int): Float = y * canvasHeight

        companion object {
            fun fromPixels(pixelX: Float, pixelY: Float, canvasWidth: Int, canvasHeight: Int, type: Int): StrokePoint {
                return StrokePoint(
                    x = pixelX / canvasWidth,    // Chuẩn hóa về 0.0-1.0
                    y = pixelY / canvasHeight,   // Chuẩn hóa về 0.0-1.0
                    type = type
                )
            }
        }
    }

    data class Stroke(
        @SerializedName("id") val id: String = UUID.randomUUID().toString(),
        @SerializedName("color") val color: Int = DEFAULT_COLOR,
        @SerializedName("width") val strokeWidth: Float = DEFAULT_STROKE_WIDTH,
        @SerializedName("isEraser") val isEraser: Boolean = false,
        @SerializedName("points") val points: MutableList<StrokePoint> = mutableListOf()
    ) {
        fun toPath(canvasWidth: Int, canvasHeight: Int): Path {
            val path = Path()
            if (points.isEmpty()) return path

            var lastX = 0f
            var lastY = 0f

            for (i in points.indices) {
                val point = points[i]
                // Chuyển đổi tọa độ chuẩn hóa sang tọa độ pixel thực tế
                val pixelX = point.getPixelX(canvasWidth)
                val pixelY = point.getPixelY(canvasHeight)

                when (point.type) {
                    MotionEvent.ACTION_DOWN -> {
                        path.moveTo(pixelX, pixelY)
                        lastX = pixelX
                        lastY = pixelY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Tạo đường cong mượt với quadTo
                        val midX = (lastX + pixelX) / 2
                        val midY = (lastY + pixelY) / 2
                        path.quadTo(lastX, lastY, midX, midY)
                        lastX = pixelX
                        lastY = pixelY
                    }
                    MotionEvent.ACTION_UP -> {
                        path.lineTo(pixelX, pixelY)
                    }
                }
            }
            return path
        }

        //getbound - tạo 1 hình chữ nhật bao quanh các nét vẽ để dễ dàng chạm được vào nét vẽ
        fun getBounds(canvasWidth: Int, canvasHeight: Int): RectF {
            val path = toPath(canvasWidth, canvasHeight)
            //tạo hình chữ nhật
            val bounds = RectF()
            path.computeBounds(bounds, true)
            // Mở rộng bounds dựa trên strokeWidth để dễ chọn
            bounds.inset(-strokeWidth - STROKE_SELECTION_TOLERANCE, -strokeWidth - STROKE_SELECTION_TOLERANCE)
            return bounds
        }
    }

    // Lưu trữ toàn bộ dữ liệu vẽ
    data class DrawingData(
        @SerializedName("strokes") val strokes: List<Stroke> = emptyList(),
        @SerializedName("width") val width: Int = 0,
        @SerializedName("height") val height: Int = 0
    )

    init {
        //scale là để zoom
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mGestureDetector = GestureDetector(context, GestureListener())

        // Đặt các thuộc tính cho view
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun isForceSaveNeeded(): Boolean {
        val result = mForceSave
        mForceSave = false
        return result
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasWidth = w
        mCanvasHeight = h

        // Tạo bitmap mới nếu cần
        if (w > 0 && h > 0) {
            if (!::mBitmap.isInitialized || mBitmap.width != w || mBitmap.height != h) {
                mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                mCanvas = Canvas(mBitmap)
            }
        }
        // Tính lại vị trí background
        calculateBackgroundRect()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Lưu trạng thái canvas
        canvas.save()

        // Áp dụng biến đổi (zoom và pan)
        canvas.translate(mPosX, mPosY)
        canvas.scale(mScaleFactor, mScaleFactor, width/2f, height/2f)

        // Vẽ nền trắng
        canvas.drawColor(Color.WHITE)

        // Vẽ ảnh nền nếu có
        mBackgroundBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, null, mBackgroundRect, mBackgroundPaint)
        }

        // Vẽ tất cả nét vẽ với tọa độ chuẩn hóa được chuyển đổi sang tọa độ thực
        for (stroke in mStrokes) {
            drawStrokeOnCanvas(canvas, stroke)
        }

        // Vẽ nét vẽ hiện tại
        mCurrentStroke?.let {
            drawStrokeOnCanvas(canvas, it)
        }

        // Khôi phục trạng thái canvas
        canvas.restore()

        // Vẽ hướng dẫn tẩy (không bị ảnh hưởng bởi biến đổi)
        if (mCurrentMode == DrawMode.ERASE) {
            val eraserRadius = mPaint.strokeWidth * 1.5f
            canvas.drawCircle(mLastTouchX, mLastTouchY, eraserRadius, mEraserHintPaint)
        }
    }

    private fun drawStrokeOnCanvas(canvas: Canvas, stroke: Stroke) {
        val path = stroke.toPath(mCanvasWidth, mCanvasHeight)
        val paint = Paint(mPaint).apply {
            color = stroke.color
            strokeWidth = stroke.strokeWidth
            if (stroke.isEraser) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                xfermode = null
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun transformTouchPoint(x: Float, y: Float): FloatArray {
        // Tạo ma trận biến đổi ngược
        val transformMatrix = Matrix()
        transformMatrix.setScale(mScaleFactor, mScaleFactor, width/2f, height/2f)
        transformMatrix.postTranslate(mPosX, mPosY)

        val inverseMatrix = Matrix()
        transformMatrix.invert(inverseMatrix)

        // Áp dụng biến đổi ngược cho tọa độ touch
        val points = floatArrayOf(x, y)
        inverseMatrix.mapPoints(points)

        return points
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Xử lý các gesture detector
        mScaleDetector.onTouchEvent(event)
        mGestureDetector.onTouchEvent(event)

        // Lưu tọa độ touch cuối cùng
        mLastTouchX = event.x
        mLastTouchY = event.y

        // Nếu đang ở chế độ PAN, để gesture detector xử lý
        if (mCurrentMode == DrawMode.PAN) {
            invalidate()
            return true
        }

        // Chuyển đổi tọa độ touch
        val points = transformTouchPoint(event.x, event.y)
        val transformedX = points[0]
        val transformedY = points[1]

        // Xử lý dựa trên chế độ hiện tại với tọa độ đã chuyển đổi
        when (mCurrentMode) {
            DrawMode.DRAW -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Bắt đầu nét vẽ mới
                        mCurrentStroke = Stroke(
                            color = mPaint.color,
                            strokeWidth = mPaint.strokeWidth,
                            isEraser = false
                        )

                        // Thêm điểm đầu tiên ĐÃ CHUẨN HÓA
                        mCurrentStroke?.points?.add(
                            StrokePoint.fromPixels(
                                transformedX, transformedY,
                                mCanvasWidth, mCanvasHeight,
                                MotionEvent.ACTION_DOWN
                            )
                        )
                        updateLastEditTime()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // Thêm điểm vào nét vẽ hiện tại
                        val lastPoint = mCurrentStroke?.points?.lastOrNull()
                        if (lastPoint != null) {
                            // Chuyển đổi lastPoint về tọa độ pixel để kiểm tra khoảng cách
                            val lastPixelX = lastPoint.getPixelX(mCanvasWidth)
                            val lastPixelY = lastPoint.getPixelY(mCanvasHeight)

                            val dx = Math.abs(transformedX - lastPixelX)
                            val dy = Math.abs(transformedY - lastPixelY)

                            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                                // Thêm điểm mới ĐÃ CHUẨN HÓA
                                mCurrentStroke?.points?.add(
                                    StrokePoint.fromPixels(
                                        transformedX, transformedY,
                                        mCanvasWidth, mCanvasHeight,
                                        MotionEvent.ACTION_MOVE
                                    )
                                )
                                updateLastEditTime()
                            }
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        // Hoàn thành nét vẽ
                        mCurrentStroke?.points?.add(
                            StrokePoint.fromPixels(
                                transformedX, transformedY,
                                mCanvasWidth, mCanvasHeight,
                                MotionEvent.ACTION_UP
                            )
                        )

                        if ((mCurrentStroke?.points?.size ?: 0) > 1) {
                            mStrokes.add(mCurrentStroke!!)
                            mForceSave = true

                            updateLastEditTime()
                            notifyDrawCompleted()
                        }

                        mCurrentStroke = null
                    }
                }
                }
            DrawMode.ERASE -> {
                // Xử lý xóa nét vẽ (chạm hoặc di chuyển)
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    // Tìm các nét vẽ gần điểm chạm
                    val strokesToErase = findStrokesNearPoint(transformedX, transformedY, mPaint.strokeWidth * 3)

                    if (strokesToErase.isNotEmpty()) {
                        var didErase = false

                        for (stroke in strokesToErase) {
                            if (mStrokes.remove(stroke)) {
                                mStrokeDeletedListener?.onStrokeDeleted(stroke.id)
                                didErase = true
                            }
                        }

                        if (didErase) {
                            mForceSave = true
                            updateLastEditTime()
                            notifyDrawCompleted()
                        }
                    }
                }
            }
            DrawMode.SELECT -> {
                if (event.action == MotionEvent.ACTION_UP) {
                    // Tìm nét vẽ tại điểm chạm
                    val strokeId = findStrokeAt(transformedX, transformedY)

                    if (strokeId != mSelectedStrokeId) {
                        mSelectedStrokeId = strokeId
                        mStrokeSelectedListener?.invoke(strokeId ?: "")
                    }
                }
            }
            else ->
            {}
        }

        invalidate()
        return true
    }


    private fun findStrokesNearPoint(x: Float, y: Float, radius: Float): List<Stroke> {
        val result = mutableListOf<Stroke>()

        for (stroke in mStrokes) {
            // Bỏ qua các nét tẩy
            if (stroke.isEraser) continue

            // Kiểm tra từng đoạn của nét vẽ
            for (i in 0 until stroke.points.size - 1) {
                val p1 = stroke.points[i]
                val p2 = stroke.points[i + 1]

                // Chuyển đổi tọa độ chuẩn hóa sang tọa độ pixel
                val p1x = p1.getPixelX(mCanvasWidth)
                val p1y = p1.getPixelY(mCanvasHeight)
                val p2x = p2.getPixelX(mCanvasWidth)
                val p2y = p2.getPixelY(mCanvasHeight)

                // Tính khoảng cách từ điểm đến đoạn thẳng
                val distance = distancePointToLineSegment(
                    x, y,
                    p1x, p1y,
                    p2x, p2y
                )

                // Nếu khoảng cách đủ gần, thêm vào kết quả
                if (distance <= radius + stroke.strokeWidth / 2) {
                    result.add(stroke)
                    break // Không cần kiểm tra các đoạn khác của nét vẽ này
                }
            }
        }

        return result
    }

    //tính khoảng cách từ điểm đến đoạn thẳng
    private fun distancePointToLineSegment(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        val A = px - x1
        val B = py - y1
        val C = x2 - x1
        val D = y2 - y1

        val dot = A * C + B * D
        val lenSq = C * C + D * D
        var param = -1f

        if (lenSq != 0f) { // Tránh chia cho 0
            param = dot / lenSq
        }

        var xx: Float
        var yy: Float

        if (param < 0) {
            xx = x1
            yy = y1
        } else if (param > 1) {
            xx = x2
            yy = y2
        } else {
            xx = x1 + param * C
            yy = y1 + param * D
        }

        val dx = px - xx
        val dy = py - yy

        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun updateLastEditTime() {
        mLastEditTime = System.currentTimeMillis()
    }

    fun getLastEditTime(): Long {
        return mLastEditTime
    }

    fun setWhiteBackground(width: Int, height: Int) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        mBackgroundBitmap = bitmap
        invalidate()
    }

    private fun findStrokeAt(x: Float, y: Float): String? {
        // Duyệt từ sau ra trước (nét vẽ sau được ưu tiên)
        for (i in mStrokes.indices.reversed()) {
            val stroke = mStrokes[i]

            // Kiểm tra xem điểm có nằm trong bounds của nét vẽ không
            val bounds = stroke.getBounds(mCanvasWidth, mCanvasHeight)
            if (bounds.contains(x, y)) {
                // Kiểm tra chi tiết hơn với path
                val path = stroke.toPath(mCanvasWidth, mCanvasHeight)
                val pathBounds = RectF()
                path.computeBounds(pathBounds, true)
                pathBounds.inset(-stroke.strokeWidth - STROKE_SELECTION_TOLERANCE, -stroke.strokeWidth - STROKE_SELECTION_TOLERANCE)

                if (pathBounds.contains(x, y)) {
                    return stroke.id
                }
            }
        }

        return null
    }

    /**
     * Vẽ một stroke lên canvas
     */
    private fun drawStroke(stroke: Stroke) {
        // Sử dụng toPath với kích thước canvas hiện tại
        val path = stroke.toPath(mCanvasWidth, mCanvasHeight)
        val paint = Paint(mPaint).apply {
            color = stroke.color
            strokeWidth = stroke.strokeWidth
            if (stroke.isEraser) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                xfermode = null
            }
        }

        mCanvas.drawPath(path, paint)
    }

    /**
     * Vẽ lại tất cả các nét
     */
    private fun redrawStrokes() {
        if (!::mCanvas.isInitialized) return

        // Xóa canvas
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Vẽ lại từng nét
        for (stroke in mStrokes) {
            drawStroke(stroke)
        }

        // Cập nhật view
        invalidate()
    }

    /**
     * Xóa một nét vẽ cụ thể
     */
    interface OnStrokeDeletedListener {
        fun onStrokeDeleted(strokeId: String)
    }

    private var mStrokeDeletedListener: OnStrokeDeletedListener? = null

    fun setOnStrokeDeletedListener(listener: OnStrokeDeletedListener) {
        mStrokeDeletedListener = listener
    }

    fun deleteStroke(stroke: Stroke) {
        if (mStrokes.contains(stroke)) {
            mStrokes.remove(stroke)
            mForceSave = true
            invalidate()
            notifyDrawCompleted()

            // Notify stroke deleted listener
            mStrokeDeletedListener?.onStrokeDeleted(stroke.id)
        }
    }


    fun clearDrawing() {
        // Remember current strokes for undo
        if (mStrokes.isNotEmpty()) {
            mDeletedStrokes.addAll(mStrokes)
        }

        // Clear strokes
        mStrokes.clear()
        mCurrentStroke = null

        // Force save and redraw
        mForceSave = true
        invalidate()
        notifyDrawCompleted()
    }

    /**
     * Thông báo vẽ hoàn thành
     */
    private fun notifyDrawCompleted() {
        mDrawCompletedListener?.onDrawCompleted()
    }

    /**
     * Đặt listener khi vẽ hoàn thành
     */
    fun setOnDrawCompletedListener(listener: OnDrawCompletedListener) {
        mDrawCompletedListener = listener
    }

    /**
     * Đặt chế độ vẽ
     */
    fun setMode(mode: DrawMode) {
        mCurrentMode = mode

        // Reset selection khi chuyển mode
        if (mode != DrawMode.SELECT) {
            mSelectedStrokeId = null
        }

        invalidate()
    }

    /**
     * Add a remote stroke (from another user)
     */
    fun addRemoteStroke(stroke: Stroke) {
        // Add the stroke to our collection
        mStrokes.add(stroke)

        // Redraw the view
        invalidate()
    }

    /**
     * Find a stroke by its ID
     */
    fun findStrokeById(id: String): Stroke? {
        return mStrokes.find { it.id == id }
    }

    /**
     * Remove a specific stroke
     */
    fun removeStroke(stroke: Stroke) {
        // Check if the stroke exists in our collection
        if (mStrokes.contains(stroke)) {
            // Add to undo stack
            val index = mStrokes.indexOf(stroke)

            // Remove the stroke
            mStrokes.remove(stroke)

            // Redraw
            redrawStrokes()

            // Important: Notify drawing completed to trigger save
            notifyDrawCompleted()
        }
    }

    /**
     * Clear without triggering local events
     */
    fun clearDrawing(triggerEvents: Boolean = true) {
        // Clear all data structures
        mStrokes.clear()
        mCurrentStroke = null
        mSelectedStrokeId = null

        // Clear canvas
        if (::mCanvas.isInitialized) {
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }

        // Reset loaded state
        pageLoaded = false


        // Update view
        invalidate()

        // Notify if needed
        if (triggerEvents) {
            notifyDrawCompleted()
        }
    }
    /**
     * Lấy chế độ vẽ hiện tại
     */
    fun getMode(): DrawMode {
        return mCurrentMode
    }

    /**
     * Đặt màu nét vẽ
     */
    fun setColor(color: Int) {
        mPaint.color = color
    }

    /**
     * Đặt độ rộng nét vẽ
     */
    fun setStrokeWidth(width: Float) {
        mPaint.strokeWidth = width
    }

    /**
     * Đặt ảnh nền
     */
    /**
     * Set a bitmap as the background image with proper sizing
     * @param bitmap The bitmap to use as background
     */

    fun setBackgroundImage(bitmap: Bitmap?) {
        // Xóa background nếu null
        if (bitmap == null) {
            mBackgroundBitmap = null
            mBackgroundWidth = 0
            mBackgroundHeight = 0
            mBackgroundRect.setEmpty()
            invalidate()
            return
        }

        // Lưu kích thước gốc
        mBackgroundWidth = bitmap.width
        mBackgroundHeight = bitmap.height

        // Lưu bitmap
        mBackgroundBitmap = bitmap

        // Tính toán vị trí
        calculateBackgroundRect()

        // Reset transformations
        resetTransform()
        invalidate()
    }

    private fun calculateBackgroundRect() {
        if (mBackgroundBitmap == null || width == 0 || height == 0) return

        // Calculate aspect ratios
        val imageRatio = mBackgroundWidth.toFloat() / mBackgroundHeight.toFloat()
        val viewRatio = width.toFloat() / height.toFloat()

        // Calculate dimensions to maintain aspect ratio
        val displayWidth: Float
        val displayHeight: Float

        if (imageRatio > viewRatio) {
            // Image is wider than view - fit to width
            displayWidth = width.toFloat()
            displayHeight = displayWidth / imageRatio
        } else {
            // Image is taller than view - fit to height
            displayHeight = height.toFloat()
            displayWidth = displayHeight * imageRatio
        }

        // Calculate position to center
        val left = (width - displayWidth) / 2f
        val top = (height - displayHeight) / 2f

        // Set the rect
        mBackgroundRect.set(left, top, left + displayWidth, top + displayHeight)

        Log.d(TAG, "Background rect: $mBackgroundRect")
    }

    private var pageLoaded = false

    fun setDrawingDataFromJson(json: String) {
        try {
            val drawingData = Gson().fromJson(json, DrawingData::class.java)

            // Clear current data
            mStrokes.clear()
            mDeletedStrokes.clear() // Also clear redo history
            mCurrentStroke = null

            // Set new data
            if (drawingData != null && drawingData.strokes != null) {
                mStrokes.addAll(drawingData.strokes)
            }

            // Force redraw
            invalidate()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting drawing data from JSON", e)
        }
    }

    fun getLastStroke(): Stroke? {
        return mStrokes.lastOrNull()
    }


    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Chỉ zoom khi ở chế độ PAN
            if (mCurrentMode == DrawMode.PAN) {
                mScaleFactor *= detector.scaleFactor

                // Giới hạn tỷ lệ zoom với giá trị hợp lý hơn
                mScaleFactor = max(MIN_SCALE, min(mScaleFactor, MAX_SCALE))

                invalidate()
                return true
            }
            return false
        }
    }

    fun resetTransform() {
        mScaleFactor = 1.0f
        mPosX = 0f
        mPosY = 0f
        calculateBackgroundRect() // Tính lại vị trí background
        invalidate()
    }
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // Chỉ pan khi ở chế độ PAN
            if (mCurrentMode == DrawMode.PAN) {
                mPosX -= distanceX
                mPosY -= distanceY

                // Thêm giới hạn để không pan quá xa
                val maxPanX = width * mScaleFactor * 0.5f
                val maxPanY = height * mScaleFactor * 0.5f

                mPosX = mPosX.coerceIn(-maxPanX, maxPanX)
                mPosY = mPosY.coerceIn(-maxPanY, maxPanY)

                invalidate()
                return true
            }
            return false
        }

        // Thêm phương thức xử lý double tap để reset view
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (mCurrentMode == DrawMode.PAN) {
                // Reset zoom và pan về giá trị mặc định
                mScaleFactor = 1.0f
                mPosX = 0f
                mPosY = 0f
                invalidate()
                return true
            }
            return false
        }
    }

}