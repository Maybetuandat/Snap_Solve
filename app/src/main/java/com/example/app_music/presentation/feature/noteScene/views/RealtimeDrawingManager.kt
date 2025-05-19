package com.example.app_music.presentation.feature.noteScene.views

import android.graphics.Color
import android.util.Log
import com.example.app_music.data.collaboration.CollaborationManager
import com.example.app_music.data.repository.StrokesRepository
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Quản lý vẽ thời gian thực và đồng bộ hóa
 */
class RealtimeDrawingManager(
    private val drawingView: DrawingView,
    private val collaborationManager: CollaborationManager,
    private val scope: CoroutineScope,
    private val currentPageId: String
) {
    private val TAG = "RealtimeDrawingManager"
    private var isLocalStroke = false
    private var activeStrokeIds = HashSet<String>() // Theo dõi ID cho trang hiện tại
    private val strokesRepository = StrokesRepository()

    // Reference trực tiếp đến node strokes trong Realtime Database
    private val strokesRef = FirebaseDatabase.getInstance().getReference("strokes").child(currentPageId)
    private var strokesListener: ChildEventListener? = null

    init {
        // Đặt listener cho các nét vẽ cục bộ
        drawingView.setOnDrawCompletedListener(object : DrawingView.OnDrawCompletedListener {
            override fun onDrawCompleted() {
                // Lấy nét vẽ cuối cùng và đồng bộ hóa
                val lastStroke = drawingView.getLastStroke()
                if (lastStroke != null && !isLocalStroke) {
                    // Đánh dấu là đang xử lý nét vẽ cục bộ để tránh vòng lặp
                    isLocalStroke = true

                    // Thêm ID nét vẽ vào tập theo dõi
                    activeStrokeIds.add(lastStroke.id)

                    // Lưu nét vẽ vào repository
                    scope.launch {
                        try {
                            val saveResult = strokesRepository.saveStroke(currentPageId, lastStroke)

                            if (saveResult.isSuccess) {
                                Log.d(TAG, "Stroke saved successfully: ${lastStroke.id}")

                                // Không cần chuyển đổi và gửi drawingAction riêng nữa
                                // vì listener thực hiện điều này tự động
                            } else {
                                Log.e(TAG, "Failed to save stroke: ${saveResult.exceptionOrNull()?.message}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving stroke", e)
                        } finally {
                            isLocalStroke = false
                        }
                    }
                }
            }
        })
        drawingView.setOnStrokeDeletedListener(object : DrawingView.OnStrokeDeletedListener {
            override fun onStrokeDeleted(strokeId: String) {
                // Remove from Firebase
                strokesRef.child(strokeId).removeValue()
                    .addOnSuccessListener {
                        Log.d(TAG, "Stroke deleted from database: $strokeId")
                        // Also remove from tracking set
                        activeStrokeIds.remove(strokeId)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to delete stroke from database: ${e.message}")
                    }
            }
        })
        // Tải các nét vẽ hiện có khi khởi tạo
        loadExistingStrokes()

        // Thiết lập listener cho các thay đổi theo thời gian thực
        setupRealtimeListener()
    }

    /**
     * Tải các nét vẽ hiện có cho trang
     */
    private fun loadExistingStrokes() {
        scope.launch {
            try {
                val result = strokesRepository.getStrokesForPage(currentPageId)

                if (result.isSuccess) {
                    val strokes = result.getOrNull() ?: emptyList()

                    if (strokes.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            // Xóa view trước khi áp dụng các nét vẽ đã tải
                            drawingView.clearDrawing(false)

                            // Áp dụng các nét vẽ hiện có theo thứ tự
                            for (stroke in strokes) {
                                Log.d(TAG, "Loading existing stroke: ${stroke.id} for page $currentPageId")

                                // Thêm vào ID đã theo dõi
                                activeStrokeIds.add(stroke.id)

                                // Thêm vào bản vẽ
                                drawingView.addRemoteStroke(stroke)
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to load strokes: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading existing strokes", e)
            }
        }
    }

    /**
     * Thiết lập listener cho các thay đổi theo thời gian thực
     */
    private fun setupRealtimeListener() {
        // Chỉ thiết lập nếu chưa có listener
        if (strokesListener != null) return

        // Tạo listener mới
        strokesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    // Kiểm tra nếu là nét vẽ của chính chúng ta
                    val createdBy = snapshot.child("createdBy").getValue(String::class.java) ?: ""
                    val strokeId = snapshot.child("id").getValue(String::class.java) ?: ""

                    // Bỏ qua nếu là nét vẽ của chúng ta hoặc đã có trong view
                    if (createdBy == collaborationManager.getCurrentUserId() ||
                        activeStrokeIds.contains(strokeId)) {
                        return
                    }

                    // Parse dữ liệu stroke
                    val color = snapshot.child("color").getValue(Long::class.java)?.toInt() ?: Color.BLACK
                    val strokeWidth = snapshot.child("strokeWidth").getValue(Double::class.java)?.toFloat() ?: 5f
                    val isEraser = snapshot.child("isEraser").getValue(Boolean::class.java) ?: false

                    // Parse dữ liệu points
                    val pointsSnapshot = snapshot.child("points")
                    val points = mutableListOf<DrawingView.StrokePoint>()

                    for (pointSnapshot in pointsSnapshot.children) {
                        val x = pointSnapshot.child("x").getValue(Double::class.java)?.toFloat() ?: 0f
                        val y = pointSnapshot.child("y").getValue(Double::class.java)?.toFloat() ?: 0f
                        val type = pointSnapshot.child("type").getValue(Int::class.java) ?: 0

                        points.add(DrawingView.StrokePoint(x, y, type))
                    }

                    // Tạo stroke
                    val stroke = DrawingView.Stroke(
                        id = strokeId,
                        color = color,
                        strokeWidth = strokeWidth,
                        isEraser = isEraser,
                        points = points
                    )

                    // Thêm vào danh sách theo dõi
                    activeStrokeIds.add(strokeId)

                    // Thêm vào view
                    scope.launch(Dispatchers.Main) {
                        drawingView.addRemoteStroke(stroke)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing added stroke", e)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Strokes thường không thay đổi sau khi tạo
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                try {
                    val strokeId = snapshot.child("id").getValue(String::class.java) ?: return

                    // Tìm stroke trong view
                    scope.launch(Dispatchers.Main) {
                        val stroke = drawingView.findStrokeById(strokeId)
                        if (stroke != null) {
                            // Xóa khỏi danh sách theo dõi
                            activeStrokeIds.remove(strokeId)

                            // Xóa khỏi view
                            drawingView.removeStroke(stroke)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling removed stroke", e)
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Không cần xử lý
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error in strokes listener", error.toException())
            }
        }

        // Đăng ký listener
        strokesRef.addChildEventListener(strokesListener!!)
    }

    /**
     * Xóa các nét vẽ hiện có khi chuyển trang
     */
    fun clearForPageChange() {
        // Hủy listener
        strokesListener?.let {
            strokesRef.removeEventListener(it)
            strokesListener = null
        }

        // Đảm bảo không nghe các sự kiện trong quá trình xóa
        isLocalStroke = true

        // Xóa hoàn toàn drawingView
        drawingView.clearDrawing(false)

        // Xóa theo dõi ID nét vẽ
        activeStrokeIds.clear()

        // Đặt lại cờ nét vẽ cục bộ
        isLocalStroke = false
    }

    /**
     * Xóa và dọn dẹp tài nguyên
     */
    fun cleanup() {
        // Hủy listener
        strokesListener?.let {
            strokesRef.removeEventListener(it)
            strokesListener = null
        }
    }
}