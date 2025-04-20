package com.example.app_music.presentation.noteScene

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.app_music.R
import com.example.app_music.presentation.noteScene.views.DrawingView
import com.example.app_music.presentation.utils.StorageManager
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.ColorPickerDialog
import java.io.File
import java.io.FileOutputStream

class NoteDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NoteDetailActivity"
    }

    private lateinit var noteTitle: TextView
    private lateinit var backButton: ImageButton
    private lateinit var drawingView: DrawingView

    // Drawing tools
    private lateinit var handButton: ImageButton
    private lateinit var penButton: ImageButton
    private lateinit var eraserButton: ImageButton
    private lateinit var colorButton: ImageButton
    private lateinit var strokeWidthButton: ImageButton
    private lateinit var undoButton: ImageButton
    private lateinit var redoButton: ImageButton

    // Help buttons
    private lateinit var helpAiButton: Button
    private lateinit var viewExplanationButton: Button

    // Drawing variables
    private var currentColor = Color.BLACK
    private var currentWidth = 5f

    // Storage manager
    private lateinit var storageManager: StorageManager

    // Note info
    private lateinit var noteId: String

    // Cập nhật phương thức onCreate trong NoteDetailActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // Get data from intent
        noteId = intent.getStringExtra("note_id") ?: ""
        val noteName = intent.getStringExtra("note_title") ?: "Unknown Note"

        // Initialize storage manager
        storageManager = StorageManager(this)

        // Initialize views
        initViews()

        // Set note info
        noteTitle.text = noteName

        // Load note image
        loadNoteImage()

        // Set up drawing view listener cho việc tự động lưu
        drawingView.setOnDrawCompletedListener(object : DrawingView.OnDrawCompletedListener {
            override fun onDrawCompleted() {
                // Cập nhật trạng thái nút undo/redo
                updateUndoRedoButtons()

                // Lưu nét vẽ ngay lập tức
                saveDrawing(false) // false = không hiển thị thông báo
            }
        })

        // Set up event listeners
        setupListeners()

        // Set default mode to PAN (hand tool)
        selectMode(DrawingView.DrawMode.PAN)

        // Update undo/redo button states
        updateUndoRedoButtons()
    }

    // Sửa lại phương thức saveDrawing trong NoteDetailActivity
    private fun saveDrawing(showToast: Boolean = true) {
        if (noteId.isEmpty()) return

        try {
            // Sử dụng Thread.sleep để đảm bảo UI được cập nhật trước
            // Tạo bitmap kết hợp cả nền và nét vẽ
            val combinedBitmap = drawingView.getCombinedBitmap()

            // Lưu bitmap theo cả hai cách để đảm bảo luôn có dữ liệu
            val success = storageManager.saveDrawingLayer(noteId, combinedBitmap) &&
                    storageManager.saveNoteImage(noteId, combinedBitmap)

            if (showToast) {
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Đã lưu bản vẽ", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Không thể lưu bản vẽ", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.d(TAG, "Tự động lưu: " + (if (success) "thành công" else "thất bại"))
            }

            // Giải phóng bitmap
            combinedBitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lưu bản vẽ: ${e.message}")
            e.printStackTrace()
        }
    }



    private fun initViews() {
        noteTitle = findViewById(R.id.text_note_title)
        backButton = findViewById(R.id.button_back_note_detail)
        drawingView = findViewById(R.id.drawing_view)

        // Drawing tools
        handButton = findViewById(R.id.button_hand)
        penButton = findViewById(R.id.button_pen)
        eraserButton = findViewById(R.id.button_eraser)
        colorButton = findViewById(R.id.button_color)
        strokeWidthButton = findViewById(R.id.button_stroke_width)
        undoButton = findViewById(R.id.button_undo)
        redoButton = findViewById(R.id.button_redo)

        // Help buttons
        helpAiButton = findViewById(R.id.button_help_ai)
        viewExplanationButton = findViewById(R.id.button_view_explanation)
    }

    private fun loadNoteImage() {
        if (noteId.isEmpty()) return

        // Hiển thị loading nếu cần
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Đang tải ảnh...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        // Tải ảnh trên thread phụ
        Thread {
            // Tải ảnh gốc
            val originalBitmap = storageManager.getNoteImageBitmap(noteId)

            // Tải lớp vẽ (nếu có)
            val drawingBitmap = storageManager.loadDrawingLayer(noteId)

            runOnUiThread {
                progressDialog.dismiss()

                if (originalBitmap != null) {
                    // Đặt ảnh gốc làm background cho drawingView
                    drawingView.setBackgroundImage(originalBitmap)

                    // Nếu có lớp vẽ, vẽ lên DrawingView
                    // (không cần làm gì thêm ở đây vì drawingView sẽ hiển thị phần vẽ của nó)

                    Toast.makeText(this, "Đã tải ảnh thành công", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Không thể tải ảnh", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun setupListeners() {
        // Cập nhật phương thức backButton.setOnClickListener trong setupListeners
        backButton.setOnClickListener {
            // Lưu trạng thái vẽ trước khi quay lại và hiển thị thông báo
            saveDrawing(true)

            // Chờ một khoảng thời gian ngắn để đảm bảo việc lưu đã được xử lý
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 300) // 300ms đủ để nhìn thấy thông báo và cảm thấy mượt mà
        }

        // Hand tool (pan/move document)
        handButton.setOnClickListener {
            selectMode(DrawingView.DrawMode.PAN)
        }

        // Pen tool
        penButton.setOnClickListener {
            selectMode(DrawingView.DrawMode.DRAW)
        }

        // Eraser tool
        eraserButton.setOnClickListener {
            selectMode(DrawingView.DrawMode.ERASE)
        }

        // Color picker
        colorButton.setOnClickListener {
            showColorPicker()
        }

        // Stroke width
        strokeWidthButton.setOnClickListener {
            showStrokeWidthDialog()
        }

        // Undo button
        undoButton.setOnClickListener {
            if (drawingView.undo()) {
                updateUndoRedoButtons()
            } else {
                Toast.makeText(this, "Không có thao tác để hoàn tác", Toast.LENGTH_SHORT).show()
            }
        }

        // Redo button
        redoButton.setOnClickListener {
            if (drawingView.redo()) {
                updateUndoRedoButtons()
            } else {
                Toast.makeText(this, "Không có thao tác để làm lại", Toast.LENGTH_SHORT).show()
            }
        }

        // Help buttons
        helpAiButton.setOnClickListener {
            Toast.makeText(this, "Chức năng trợ giúp đang phát triển", Toast.LENGTH_SHORT).show()
        }

        viewExplanationButton.setOnClickListener {
            Toast.makeText(this, "Chức năng giải thích đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectMode(mode: DrawingView.DrawMode) {
        // Set the drawing mode
        drawingView.setMode(mode)

        // Update UI to highlight selected tool
        handButton.setBackgroundResource(if (mode == DrawingView.DrawMode.PAN) R.drawable.tool_selected_bg else android.R.color.transparent)
        penButton.setBackgroundResource(if (mode == DrawingView.DrawMode.DRAW) R.drawable.tool_selected_bg else android.R.color.transparent)
        eraserButton.setBackgroundResource(if (mode == DrawingView.DrawMode.ERASE) R.drawable.tool_selected_bg else android.R.color.transparent)

        // Configure drawing view based on mode
        when (mode) {
            DrawingView.DrawMode.DRAW -> {
                drawingView.setColor(currentColor)
                drawingView.setStrokeWidth(currentWidth)
            }
            DrawingView.DrawMode.ERASE -> {
                drawingView.setStrokeWidth(currentWidth * 2) // Eraser is thicker than pen
            }
            DrawingView.DrawMode.PAN -> {
                // No special configuration needed
            }
        }
    }

    private fun showColorPicker() {
        ColorPickerDialog.Builder(this)
            .setTitle("Chọn màu sắc")
            .setPreferenceName("NoteColorPickerDialog")
            .setPositiveButton("Chọn", ColorEnvelopeListener { envelope, _ ->
                currentColor = envelope.color
                if (drawingView.getMode() == DrawingView.DrawMode.DRAW) {
                    drawingView.setColor(currentColor)
                }

                // Update color button tint
                colorButton.setColorFilter(currentColor)
            })
            .setNegativeButton("Hủy") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .attachAlphaSlideBar(true)
            .attachBrightnessSlideBar(true)
            .setBottomSpace(12)
            .show()
    }

    private fun showStrokeWidthDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_stroke_width, null)

        // Lấy tham chiếu đến các view
        val seekBar = dialogView.findViewById<SeekBar>(R.id.stroke_width_seekbar)
        val previewView = dialogView.findViewById<View>(R.id.stroke_preview)
        val widthValueText = dialogView.findViewById<TextView>(R.id.width_value_text)

        // Các nút preset
        val btnThin = dialogView.findViewById<TextView>(R.id.btn_thin)
        val btnMedium = dialogView.findViewById<TextView>(R.id.btn_medium)
        val btnThick = dialogView.findViewById<TextView>(R.id.btn_thick)

        // Các nút hành động
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
        val btnApply = dialogView.findViewById<TextView>(R.id.btn_apply)

        // Khởi tạo giá trị
        seekBar.progress = currentWidth.toInt()
        updateStrokePreview(previewView, currentWidth, currentColor)
        widthValueText.text = "Độ dày: ${currentWidth.toInt()}"

        // Tạo dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Thiết lập không có title mặc định
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Thiết lập listener cho SeekBar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val width = progress.coerceAtLeast(1).toFloat()
                widthValueText.text = "Độ dày: ${width.toInt()}"
                updateStrokePreview(previewView, width, currentColor)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Thiết lập listener cho các nút preset
        btnThin.setOnClickListener {
            val thinWidth = 2f
            seekBar.progress = thinWidth.toInt()
            widthValueText.text = "Độ dày: ${thinWidth.toInt()}"
            updateStrokePreview(previewView, thinWidth, currentColor)

            // Cập nhật trạng thái các nút
            updatePresetButtonsState(btnThin, btnMedium, btnThick)
        }

        btnMedium.setOnClickListener {
            val mediumWidth = 8f
            seekBar.progress = mediumWidth.toInt()
            widthValueText.text = "Độ dày: ${mediumWidth.toInt()}"
            updateStrokePreview(previewView, mediumWidth, currentColor)

            // Cập nhật trạng thái các nút
            updatePresetButtonsState(btnMedium, btnThin, btnThick)
        }

        btnThick.setOnClickListener {
            val thickWidth = 15f
            seekBar.progress = thickWidth.toInt()
            widthValueText.text = "Độ dày: ${thickWidth.toInt()}"
            updateStrokePreview(previewView, thickWidth, currentColor)

            // Cập nhật trạng thái các nút
            updatePresetButtonsState(btnThick, btnThin, btnMedium)
        }

        // Thiết lập listener cho các nút hành động
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnApply.setOnClickListener {
            currentWidth = seekBar.progress.coerceAtLeast(1).toFloat()

            // Cập nhật độ dày nét vẽ
            if (drawingView.getMode() == DrawingView.DrawMode.DRAW) {
                drawingView.setStrokeWidth(currentWidth)
            } else if (drawingView.getMode() == DrawingView.DrawMode.ERASE) {
                drawingView.setStrokeWidth(currentWidth * 2)
            }

            dialog.dismiss()
        }

        // Thiết lập dialog window để có bo góc
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Thiết lập kích thước
            val params = window.attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = params
        }

        // Hiển thị dialog
        dialog.show()
    }

    // Cập nhật trạng thái hiển thị của các nút preset
    private fun updatePresetButtonsState(selectedButton: TextView, vararg otherButtons: TextView) {
        // Cập nhật nút được chọn
        selectedButton.setBackgroundResource(R.drawable.button_blue)
        selectedButton.setTextColor(Color.WHITE)

        // Cập nhật các nút khác
        for (button in otherButtons) {
            button.setBackgroundResource(R.drawable.button_rounded_white)
            button.setTextColor(Color.parseColor("#333333"))
        }
    }

    private fun updateStrokePreview(view: View, width: Float, color: Int) {
        // Cập nhật chiều cao của view để thể hiện độ dày nét vẽ
        val params = view.layoutParams
        params.height = width.toInt()
        view.layoutParams = params

        // Cập nhật màu sắc
        view.setBackgroundColor(color)
    }

    private fun updateUndoRedoButtons() {
        // Update undo/redo button states
        undoButton.isEnabled = drawingView.canUndo()
        undoButton.alpha = if (drawingView.canUndo()) 1.0f else 0.5f

        redoButton.isEnabled = drawingView.canRedo()
        redoButton.alpha = if (drawingView.canRedo()) 1.0f else 0.5f
    }

    private fun saveDrawing() {
        if (noteId.isEmpty()) return

        // Lấy bitmap chứa các nét vẽ
        val drawingBitmap = drawingView.getDrawingBitmap()

        // Lưu bitmap vào bộ nhớ
        Thread {
            val success = storageManager.saveDrawingLayer(noteId, drawingBitmap)

            runOnUiThread {
                if (success) {
                    Log.d(TAG, "Đã lưu bản vẽ thành công")
                } else {
                    Log.e(TAG, "Lỗi khi lưu bản vẽ")
                }
            }

            // Giải phóng bitmap
            drawingBitmap.recycle()
        }.start()
    }

    override fun onPause() {
        super.onPause()
        // Lưu bản vẽ khi rời khỏi màn hình
        saveDrawing()
    }
}