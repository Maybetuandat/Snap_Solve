package com.example.app_music.presentation.feature.noteScene.views

import android.content.Context
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

class RealtimeDrawingManager(
    private val drawingView: DrawingView,
    private val collaborationManager: CollaborationManager,
    private val scope: CoroutineScope,
    private val currentPageId: String,
    private val context: Context
)
{
    private val TAG = "RealtimeDrawingManager"
    private var isLocalStroke = false
    private var activeStrokeIds = HashSet<String>()
    private val strokesRepository = StrokesRepository(context)

    private val strokesRef = FirebaseDatabase.getInstance().getReference("strokes").child(currentPageId)
    private var strokesListener: ChildEventListener? = null

    init {
        // Đặt listener khi vẽ xong
        drawingView.setOnDrawCompletedListener(object : DrawingView.OnDrawCompletedListener {
            override fun onDrawCompleted() {
                // Lấy nét vẽ cuối cùng và đồng bộ hóa
                val lastStroke = drawingView.getLastStroke()
                if (lastStroke != null && !isLocalStroke) {
                    // Đánh dấu là đang xử lý nét vẽ cục bộ để tránh vòng lặp
                    isLocalStroke = true

                    // Thêm ID nét vẽ vào tập theo dõi
                    activeStrokeIds.add(lastStroke.id)

                    // Lưu nét vẽ vào repository, tạo 1 couroutine riêng để không chặn luồng UI
                    scope.launch {
                        try {
                            val saveResult = strokesRepository.saveStroke(currentPageId, lastStroke)

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
                        // Also remove from tracking set
                        activeStrokeIds.remove(strokeId)
                    }
                    .addOnFailureListener { e ->
                    }
            }
        })

        loadExistingStrokes()

        //set up các listener
        setupRealtimeListener()
    }

    //tải các nét vẽ cho trang
    private fun loadExistingStrokes() {
        scope.launch {
            try {
                val result = strokesRepository.getStrokesForPage(currentPageId)

                if (result.isSuccess) {
                    val strokes = result.getOrNull() ?: emptyList()

                    if (strokes.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            // Xóa view
                            drawingView.clearDrawing(false)

                            for (stroke in strokes) {
                                // Thêm id stroke vào list để theo dõi
                                activeStrokeIds.add(stroke.id)
                                // vẽ các nét
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

    private fun setupRealtimeListener() {
        if (strokesListener != null) return

        strokesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    // Kiểm tra nếu là nét vẽ của mình
                    val createdBy = snapshot.child("createdBy").getValue(String::class.java) ?: ""
                    val strokeId = snapshot.child("id").getValue(String::class.java) ?: ""

                    // Bỏ qua nếu là nét vẽ của mình hoặc đã vẽ rồi
                    if (createdBy == collaborationManager.getCurrentUserId() ||
                        activeStrokeIds.contains(strokeId)) {
                        return
                    }

                    // Parse dữ liệu stroke
                    val color = snapshot.child("color").getValue(Long::class.java)?.toInt() ?: Color.BLACK
                    val strokeWidth = snapshot.child("strokeWidth").getValue(Double::class.java)?.toFloat() ?: 5f
                    val isEraser = snapshot.child("isEraser").getValue(Boolean::class.java) ?: false

                    // Parse dữ liệu points - sử dụng tọa độ chuẩn hóa
                    val pointsSnapshot = snapshot.child("points")
                    val points = mutableListOf<DrawingView.StrokePoint>()

                    for (pointSnapshot in pointsSnapshot.children) {
                        val x = pointSnapshot.child("x").getValue(Double::class.java)?.toFloat() ?: 0f
                        val y = pointSnapshot.child("y").getValue(Double::class.java)?.toFloat() ?: 0f
                        val type = pointSnapshot.child("type").getValue(Int::class.java) ?: 0

                        // Tạo điểm với tọa độ chuẩn hóa
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
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error in strokes listener", error.toException())
            }
        }

        // Đăng ký listener
        strokesRef.addChildEventListener(strokesListener!!)
    }

    //xóa và dọn dẹp
    fun cleanup() {
        // Hủy listener
        strokesListener?.let {
            strokesRef.removeEventListener(it)
            strokesListener = null
        }
    }
}