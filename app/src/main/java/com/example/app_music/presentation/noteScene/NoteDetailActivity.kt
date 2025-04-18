package com.example.app_music.presentation.noteScene

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.app_music.R
import com.example.app_music.presentation.noteScene.views.DrawingView

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var noteTitle: TextView
    private lateinit var backButton: ImageButton
    private lateinit var noteImageView: ImageView
    private lateinit var drawingView: DrawingView

    // Công cụ vẽ
    private lateinit var handButton: ImageButton
    private lateinit var penButton: ImageButton
    private lateinit var eraserButton: ImageButton
    private lateinit var colorButton: ImageButton
    private lateinit var helpButton: ImageButton

    // Các biến cho việc vẽ
    private var currentTool = Tool.HAND
    private var currentColor = Color.BLACK
    private var currentWidth = 5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // Lấy dữ liệu từ intent
        val noteId = intent.getStringExtra("note_id") ?: ""
        val noteName = intent.getStringExtra("note_title") ?: "Unknown Note"

        // Khởi tạo các view
        initViews()

        // Hiển thị thông tin note
        noteTitle.text = noteName

        // Khởi tạo các sự kiện
        setupListeners()

        // Mặc định chọn công cụ Hand (di chuyển)
        selectTool(Tool.HAND)
    }

    private fun initViews() {
        noteTitle = findViewById(R.id.text_note_title)
        backButton = findViewById(R.id.button_back_note_detail)
        noteImageView = findViewById(R.id.image_note)
        drawingView = findViewById(R.id.drawing_view)

        // Công cụ vẽ
        handButton = findViewById(R.id.button_hand)
        penButton = findViewById(R.id.button_pen)
        eraserButton = findViewById(R.id.button_eraser)
        colorButton = findViewById(R.id.button_color)
        helpButton = findViewById(R.id.button_help)
    }

    private fun setupListeners() {
        // Nút Back
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Công cụ Hand
        handButton.setOnClickListener {
            selectTool(Tool.HAND)
        }

        // Công cụ Pen
        penButton.setOnClickListener {
            selectTool(Tool.PEN)
        }

        // Công cụ Eraser
        eraserButton.setOnClickListener {
            selectTool(Tool.ERASER)
        }

        // Nút chọn màu
        colorButton.setOnClickListener {
            showColorPicker()
        }

        // Nút help
        helpButton.setOnClickListener {
            showHelpOptions()
        }
    }

    private fun selectTool(tool: Tool) {
        // Đặt công cụ hiện tại
        currentTool = tool

        // Cập nhật giao diện các nút
        handButton.setBackgroundResource(if (tool == Tool.HAND) R.drawable.tool_selected_bg else android.R.color.transparent)
        penButton.setBackgroundResource(if (tool == Tool.PEN) R.drawable.tool_selected_bg else android.R.color.transparent)
        eraserButton.setBackgroundResource(if (tool == Tool.ERASER) R.drawable.tool_selected_bg else android.R.color.transparent)

        // Cập nhật chế độ của DrawingView
        when (tool) {
            Tool.HAND -> {
                drawingView.isEnabled = false
                drawingView.visibility = View.INVISIBLE
            }
            Tool.PEN -> {
                drawingView.isEnabled = true
                drawingView.visibility = View.VISIBLE
                drawingView.setColor(currentColor)
                drawingView.setStrokeWidth(currentWidth)
                drawingView.enableEraser(false)
            }
            Tool.ERASER -> {
                drawingView.isEnabled = true
                drawingView.visibility = View.VISIBLE
                drawingView.enableEraser(true)
                drawingView.setStrokeWidth(currentWidth * 2) // Eraser lớn hơn bút
            }
        }
    }

    private fun showColorPicker() {
        val colorPickerDialog = ColorPickerDialog(this)
        colorPickerDialog.setOnColorSelectedListener { color ->
            currentColor = color
            if (currentTool == Tool.PEN) {
                drawingView.setColor(color)
            }

            // Cập nhật icon của nút màu
            colorButton.setColorFilter(color)
        }
        colorPickerDialog.show()
    }

    private fun showHelpOptions() {
        // Hiển thị các tùy chọn hỗ trợ
        // Có thể là một dialog hoặc menu popup
    }

    // Enum cho các công cụ
    enum class Tool {
        HAND, PEN, ERASER
    }
}