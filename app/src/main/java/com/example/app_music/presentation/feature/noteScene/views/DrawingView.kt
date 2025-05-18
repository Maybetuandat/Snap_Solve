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
import androidx.core.graphics.withSave
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/**
 * View vẽ với các tính năng:
 * - Vẽ, tẩy, di chuyển, zoom
 * - Undo/redo
 * - Lưu và tải lại các nét vẽ dưới dạng vector
 * - Xóa nét vẽ cụ thể
 */
class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class DrawMode {
        DRAW,   // Chế độ vẽ
        ERASE,  // Chế độ tẩy
        PAN,    // Chế độ di chuyển/zoom
        SELECT  // Chế độ chọn nét vẽ
    }
    companion object {
        private const val TAG = "DrawingView"
        private const val DEFAULT_STROKE_WIDTH = 5f
        private const val DEFAULT_COLOR = Color.BLACK
        private const val TOUCH_TOLERANCE = 4f
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 5.0f
        private const val STROKE_SELECTION_TOLERANCE = 20f // Dung sai khi chọn nét vẽ
    }

    // Paint cho vẽ
    private val mPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = DEFAULT_COLOR
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = DEFAULT_STROKE_WIDTH
    }

    // Bitmap cho canvas
    private lateinit var mBitmap: Bitmap
    private lateinit var mCanvas: Canvas

    // Các biến lưu trạng thái vẽ
    private var mCurrentMode = DrawMode.DRAW
    private var mSelectedStrokeId: String? = null
    private var mHighlightPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLUE
        strokeWidth = 2f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    // Các biến cho zooming và panning
    private var mPosX = 0f
    private var mPosY = 0f
    private var mScaleFactor = 1f
    private val mScaleDetector: ScaleGestureDetector
    private val mGestureDetector: GestureDetector

    // Background image
    private var mBackgroundBitmap: Bitmap? = null
    private val mBackgroundPaint = Paint().apply {
        isFilterBitmap = true
    }

    // Listeners
    interface OnDrawCompletedListener {
        fun onDrawCompleted()
    }
    private var mDrawCompletedListener: OnDrawCompletedListener? = null
    private var mStrokeSelectedListener: ((String) -> Unit)? = null

    // Danh sách các nét vẽ
    private val mStrokes = mutableListOf<Stroke>()
    private var mCurrentStroke: Stroke? = null

    // Undo/Redo
    private val mUndoStack = mutableListOf<DrawAction>()
    private val mRedoStack = mutableListOf<DrawAction>()

    // Lớp dữ liệu cho nét vẽ
    data class StrokePoint(
        @SerializedName("x") val x: Float = 0f,
        @SerializedName("y") val y: Float = 0f,
        @SerializedName("type") val type: Int = MotionEvent.ACTION_MOVE // ACTION_DOWN, ACTION_MOVE, ACTION_UP
    )

    data class Stroke(
        @SerializedName("id") val id: String = UUID.randomUUID().toString(),
        @SerializedName("color") val color: Int = DEFAULT_COLOR,
        @SerializedName("width") val strokeWidth: Float = DEFAULT_STROKE_WIDTH,
        @SerializedName("isEraser") val isEraser: Boolean = false,
        @SerializedName("points") val points: MutableList<StrokePoint> = mutableListOf()
    ) {
        fun toPath(): Path {
            val path = Path()
            if (points.isEmpty()) return path

            var lastX = 0f
            var lastY = 0f

            for (i in points.indices) {
                val point = points[i]
                when (point.type) {
                    MotionEvent.ACTION_DOWN -> {
                        path.moveTo(point.x, point.y)
                        lastX = point.x
                        lastY = point.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Tạo đường cong mượt với quadTo
                        val midX = (lastX + point.x) / 2
                        val midY = (lastY + point.y) / 2
                        path.quadTo(lastX, lastY, midX, midY)
                        lastX = point.x
                        lastY = point.y
                    }
                    MotionEvent.ACTION_UP -> {
                        path.lineTo(point.x, point.y)
                    }
                }
            }
            return path
        }

        // Tính toán bounds của stroke với strokeWidth
        fun getBounds(): RectF {
            val path = toPath()
            val bounds = RectF()
            path.computeBounds(bounds, true)
            // Mở rộng bounds dựa trên strokeWidth để dễ chọn
            bounds.inset(-strokeWidth - STROKE_SELECTION_TOLERANCE, -strokeWidth - STROKE_SELECTION_TOLERANCE)
            return bounds
        }
    }

    // Lớp dữ liệu để lưu trữ toàn bộ bản vẽ
    data class DrawingData(
        @SerializedName("strokes") val strokes: List<Stroke> = emptyList(),
        @SerializedName("width") val width: Int = 0,
        @SerializedName("height") val height: Int = 0
    )

    // Lớp cơ sở cho Undo/Redo
    sealed class DrawAction {
        data class AddStroke(val stroke: Stroke) : DrawAction()
        data class RemoveStroke(val stroke: Stroke, val index: Int) : DrawAction()
        data class ClearAll(val strokes: List<Stroke>) : DrawAction()
    }

    init {
        // Khởi tạo gesture detectors
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mGestureDetector = GestureDetector(context, GestureListener())

        // Đặt các thuộc tính cho view
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Tạo bitmap mới khi kích thước view thay đổi
        if (w > 0 && h > 0) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            mCanvas = Canvas(mBitmap)
        }

        // Vẽ lại tất cả nét vẽ
        redrawStrokes()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Lưu trạng thái canvas
        canvas.withSave {
            // Áp dụng transformations cho zoom và pan
            translate(mPosX, mPosY)
            scale(mScaleFactor, mScaleFactor)

            // Vẽ background image nếu có
            mBackgroundBitmap?.let {
                // Tính vị trí để đặt ảnh giữa
                val left = (width - it.width) / 2f
                val top = (height - it.height) / 2f
                drawBitmap(it, left, top, mBackgroundPaint)
            }

            // Vẽ bitmap với tất cả nét vẽ đã vẽ
            drawBitmap(mBitmap, 0f, 0f, null)

            // Vẽ nét vẽ hiện tại đang được vẽ
            mCurrentStroke?.let { stroke ->
                val path = stroke.toPath()
                val paint = Paint(mPaint).apply {
                    color = stroke.color
                    strokeWidth = stroke.strokeWidth
                    if (stroke.isEraser) {
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    }
                }
                drawPath(path, paint)
            }

            // Vẽ viền highlight cho nét vẽ được chọn
            if (mCurrentMode == DrawMode.SELECT && mSelectedStrokeId != null) {
                val selectedStroke = mStrokes.find { it.id == mSelectedStrokeId }
                selectedStroke?.let {
                    val path = it.toPath()
                    drawPath(path, mHighlightPaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Đầu tiên cho ScaleGestureDetector xử lý
        mScaleDetector.onTouchEvent(event)

        // Nếu đang multitouch (zoom), ưu tiên xử lý đó
        if (event.pointerCount > 1) {
            return true
        }

        // Xử lý touch dựa trên chế độ hiện tại
        when (mCurrentMode) {
            DrawMode.PAN -> {
                // Để gesture detector xử lý panning
                mGestureDetector.onTouchEvent(event)
                invalidate()
                return true
            }
            DrawMode.DRAW, DrawMode.ERASE -> {
                handleDrawingTouch(event)
                invalidate()
                return true
            }
            DrawMode.SELECT -> {
                handleSelectionTouch(event)
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Xử lý sự kiện touch khi ở chế độ vẽ hoặc tẩy
     */
    private fun handleDrawingTouch(event: MotionEvent) {
        // Điều chỉnh tọa độ theo scale và pan
        val adjustedX = (event.x - mPosX) / mScaleFactor
        val adjustedY = (event.y - mPosY) / mScaleFactor

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Bỏ chọn nét vẽ nếu có
                mSelectedStrokeId = null

                // Bắt đầu nét vẽ mới
                mCurrentStroke = Stroke(
                    color = if (mCurrentMode == DrawMode.ERASE) Color.TRANSPARENT else mPaint.color,
                    strokeWidth = mPaint.strokeWidth,
                    isEraser = (mCurrentMode == DrawMode.ERASE)
                )

                // Thêm điểm đầu tiên
                mCurrentStroke?.points?.add(StrokePoint(adjustedX, adjustedY, MotionEvent.ACTION_DOWN))
            }

            MotionEvent.ACTION_MOVE -> {
                // Kiểm tra xem đã di chuyển đủ xa chưa
                val lastPoint = mCurrentStroke?.points?.lastOrNull()
                if (lastPoint != null) {
                    val dx = abs(adjustedX - lastPoint.x)
                    val dy = abs(adjustedY - lastPoint.y)

                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        // Thêm điểm mới vào nét vẽ hiện tại
                        mCurrentStroke?.points?.add(StrokePoint(adjustedX, adjustedY, MotionEvent.ACTION_MOVE))
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                // Thêm điểm cuối vào nét vẽ
                mCurrentStroke?.points?.add(StrokePoint(adjustedX, adjustedY, MotionEvent.ACTION_UP))

                // Thêm nét vẽ vào danh sách nếu có ít nhất 2 điểm
                mCurrentStroke?.let { stroke ->
                    if (stroke.points.size > 1) {
                        mStrokes.add(stroke)

                        // Thêm vào stack undo
                        mUndoStack.add(DrawAction.AddStroke(stroke))
                        mRedoStack.clear()

                        // Vẽ nét lên canvas
                        drawStroke(stroke)

                        // Thông báo hoàn thành vẽ
                        notifyDrawCompleted()
                    }
                }

                // Reset nét vẽ hiện tại
                mCurrentStroke = null
            }
        }
    }

    /**
     * Xử lý sự kiện touch khi ở chế độ chọn
     */
    private fun handleSelectionTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                // Điều chỉnh tọa độ theo scale và pan
                val adjustedX = (event.x - mPosX) / mScaleFactor
                val adjustedY = (event.y - mPosY) / mScaleFactor

                // Chọn nét vẽ ở vị trí này
                val strokeId = findStrokeAt(adjustedX, adjustedY)
                if (strokeId != mSelectedStrokeId) {
                    mSelectedStrokeId = strokeId
                    mStrokeSelectedListener?.invoke(strokeId ?: "")
                    invalidate()
                }
            }
        }
    }

    /**
     * Tìm nét vẽ ở vị trí được chỉ định
     */
    private fun findStrokeAt(x: Float, y: Float): String? {
        // Duyệt từ sau ra trước (nét vẽ sau được ưu tiên)
        for (i in mStrokes.indices.reversed()) {
            val stroke = mStrokes[i]

            // Kiểm tra xem điểm có nằm trong bounds của nét vẽ không
            val bounds = stroke.getBounds()
            if (bounds.contains(x, y)) {
                // Kiểm tra chi tiết hơn với path
                val path = stroke.toPath()
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
        val path = stroke.toPath()
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
    fun deleteSelectedStroke() {
        mSelectedStrokeId?.let { id ->
            val index = mStrokes.indexOfFirst { it.id == id }

            if (index != -1) {
                val stroke = mStrokes.removeAt(index)

                // Thêm vào stack undo
                mUndoStack.add(DrawAction.RemoveStroke(stroke, index))
                mRedoStack.clear()

                // Reset selection
                mSelectedStrokeId = null

                // Vẽ lại tất cả
                redrawStrokes()

                // Thông báo hoàn thành
                notifyDrawCompleted()
            }
        }
    }

    /**
     * Undo thao tác gần nhất
     */
    fun undo(): Boolean {
        if (mUndoStack.isEmpty()) return false

        val action = mUndoStack.removeAt(mUndoStack.size - 1)
        mRedoStack.add(action)

        when (action) {
            is DrawAction.AddStroke -> {
                // Xóa nét vẽ đã thêm
                mStrokes.removeIf { it.id == action.stroke.id }
            }
            is DrawAction.RemoveStroke -> {
                // Thêm lại nét vẽ đã xóa
                if (action.index <= mStrokes.size) {
                    mStrokes.add(action.index, action.stroke)
                } else {
                    mStrokes.add(action.stroke)
                }
            }
            is DrawAction.ClearAll -> {
                // Thêm lại tất cả nét vẽ đã xóa
                mStrokes.addAll(action.strokes)
            }
        }

        // Vẽ lại tất cả
        redrawStrokes()

        // Thông báo hoàn thành
        notifyDrawCompleted()

        return true
    }

    /**
     * Redo thao tác đã undo
     */
    fun redo(): Boolean {
        if (mRedoStack.isEmpty()) return false

        val action = mRedoStack.removeAt(mRedoStack.size - 1)
        mUndoStack.add(action)

        when (action) {
            is DrawAction.AddStroke -> {
                // Thêm lại nét vẽ
                mStrokes.add(action.stroke)
            }
            is DrawAction.RemoveStroke -> {
                // Xóa lại nét vẽ
                mStrokes.removeIf { it.id == action.stroke.id }
            }
            is DrawAction.ClearAll -> {
                // Xóa tất cả nét vẽ
                mStrokes.clear()
            }
        }

        // Vẽ lại tất cả
        redrawStrokes()

        // Thông báo hoàn thành
        notifyDrawCompleted()

        return true
    }

    /**
     * Xóa tất cả nét vẽ
     */
    fun clearDrawing() {
        // Lưu trạng thái trước khi xóa
        if (mStrokes.isNotEmpty()) {
            val allStrokes = ArrayList(mStrokes)
            mUndoStack.add(DrawAction.ClearAll(allStrokes))
            mRedoStack.clear()
        }

        // Xóa tất cả nét vẽ
        mStrokes.clear()

        // Reset selection
        mSelectedStrokeId = null

        // Xóa canvas
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Cập nhật view
        invalidate()

        // Thông báo hoàn thành
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
     * Đặt listener khi chọn nét vẽ
     */
    fun setOnStrokeSelectedListener(listener: (String) -> Unit) {
        mStrokeSelectedListener = listener
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
    fun setBackgroundImage(bitmap: Bitmap) {
        mBackgroundBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        invalidate()
    }

    /**
     * Lấy dữ liệu vẽ
     */
    fun getDrawingData(): DrawingData {
        return DrawingData(
            strokes = mStrokes.toList(),
            width = width,
            height = height
        )
    }

    /**
     * Khôi phục dữ liệu vẽ
     */
    fun setDrawingData(drawingData: DrawingData) {
        // Xóa tất cả nét vẽ hiện tại
        mStrokes.clear()
        mUndoStack.clear()
        mRedoStack.clear()

        // Thêm các nét vẽ từ dữ liệu
        mStrokes.addAll(drawingData.strokes)

        // Vẽ lại tất cả
        redrawStrokes()
    }

    /**
     * Chuyển đổi dữ liệu vẽ sang JSON
     */
    fun getDrawingDataAsJson(): String {
        return Gson().toJson(getDrawingData())
    }

    /**
     * Khôi phục dữ liệu vẽ từ JSON
     */
    fun setDrawingDataFromJson(json: String) {
        try {
            val drawingData = Gson().fromJson(json, DrawingData::class.java)
            setDrawingData(drawingData)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting drawing data from JSON", e)
        }
    }

    /**
     * Lấy bitmap của bản vẽ
     */
    fun getDrawingBitmap(): Bitmap {
        // Tạo bitmap mới cho bản vẽ (không bao gồm background)
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Vẽ các nét
        canvas.drawBitmap(mBitmap, 0f, 0f, null)

        return resultBitmap
    }

    /**
     * Lấy bitmap kết hợp cả background và bản vẽ
     */
    fun getCombinedBitmap(): Bitmap {
        // Tạo bitmap mới
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Vẽ background nếu có
        mBackgroundBitmap?.let {
            val left = (width - it.width) / 2f
            val top = (height - it.height) / 2f
            canvas.drawBitmap(it, left, top, mBackgroundPaint)
        }

        // Vẽ các nét
        canvas.drawBitmap(mBitmap, 0f, 0f, null)

        return resultBitmap
    }

    /**
     * Kiểm tra có thể undo không
     */
    fun canUndo(): Boolean = mUndoStack.isNotEmpty()

    /**
     * Kiểm tra có thể redo không
     */
    fun canRedo(): Boolean = mRedoStack.isNotEmpty()
    fun getLastStroke(): Stroke? {
        return mStrokes.lastOrNull()
    }

    /**
     * Lấy thông tin nét vẽ hiện tại (đang vẽ dở)
     */
    fun getCurrentStroke(): Stroke? {
        return mCurrentStroke
    }
    /**
     * Scale listener cho pinch-to-zoom
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor

            // Giới hạn tỷ lệ zoom
            mScaleFactor = max(MIN_SCALE, min(mScaleFactor, MAX_SCALE))

            invalidate()
            return true
        }
    }

    /**
     * Gesture listener cho panning
     */
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
                invalidate()
                return true
            }
            return false
        }
    }
}