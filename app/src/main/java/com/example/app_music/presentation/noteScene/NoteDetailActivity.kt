package com.example.app_music.presentation.noteScene

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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
import com.example.app_music.R
import com.example.app_music.presentation.noteScene.views.DrawingView
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var noteTitle: TextView
    private lateinit var backButton: ImageButton
    private lateinit var noteImageView: ImageView
    private lateinit var drawingView: DrawingView

    // Drawing tools
    private lateinit var handButton: ImageButton
    private lateinit var penButton: ImageButton
    private lateinit var eraserButton: ImageButton
    private lateinit var colorButton: ImageButton
    private lateinit var strokeWidthButton: ImageButton
    private lateinit var undoButton: ImageButton
    private lateinit var redoButton: ImageButton

    // Drawing variables
    private var currentColor = Color.BLACK
    private var currentWidth = 5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // Get data from intent
        val noteId = intent.getStringExtra("note_id") ?: ""
        val noteName = intent.getStringExtra("note_title") ?: "Unknown Note"

        // Initialize views
        initViews()

        // Set note info
        noteTitle.text = noteName

        // Set up drawing view listener for undo/redo button updates
        drawingView.setOnDrawCompletedListener(object : DrawingView.OnDrawCompletedListener {
            override fun onDrawCompleted() {
                updateUndoRedoButtons()
            }
        })

        // Set up event listeners
        setupListeners()

        // Set default mode to PAN (hand tool)
        selectMode(DrawingView.DrawMode.PAN)

        // Update undo/redo button states
        updateUndoRedoButtons()
    }

    private fun initViews() {
        noteTitle = findViewById(R.id.text_note_title)
        backButton = findViewById(R.id.button_back_note_detail)
        noteImageView = findViewById(R.id.image_note)
        drawingView = findViewById(R.id.drawing_view)

        // Drawing tools
        handButton = findViewById(R.id.button_hand)
        penButton = findViewById(R.id.button_pen)
        eraserButton = findViewById(R.id.button_eraser)
        colorButton = findViewById(R.id.button_color)
        strokeWidthButton = findViewById(R.id.button_stroke_width)
        undoButton = findViewById(R.id.button_undo)
        redoButton = findViewById(R.id.button_redo)

        // Set up help buttons at bottom
        val helpAiButton = findViewById<Button>(R.id.button_help_ai)
        val explanationButton = findViewById<Button>(R.id.button_view_explanation)

        helpAiButton.setOnClickListener {
            Toast.makeText(this, "Đang phát triển tính năng trợ giúp", Toast.LENGTH_SHORT).show()
        }

        explanationButton.setOnClickListener {
            Toast.makeText(this, "Đang phát triển tính năng giải thích", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        // Back button
        backButton.setOnClickListener {
            onBackPressed()
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
                drawingView.isEnabled = true
                drawingView.visibility = View.VISIBLE
                drawingView.setColor(currentColor)
                drawingView.setStrokeWidth(currentWidth)
            }
            DrawingView.DrawMode.ERASE -> {
                drawingView.isEnabled = true
                drawingView.visibility = View.VISIBLE
                drawingView.setStrokeWidth(currentWidth * 2) // Eraser is thicker than pen
            }
            DrawingView.DrawMode.PAN -> {
                drawingView.isEnabled = true
                drawingView.visibility = View.VISIBLE
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

    override fun onResume() {
        super.onResume()
        updateUndoRedoButtons()
    }
}