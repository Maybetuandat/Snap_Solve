package com.example.app_music.presentation.feature.noteScene

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.SeekBar
import android.widget.TextView
import com.example.app_music.R

class StrokeWidthDialog(
    context: Context,
    private val initialWidth: Float = 5f
) : Dialog(context) {
    
    private var onStrokeWidthSelectedListener: ((Float) -> Unit)? = null
    private var selectedWidth = initialWidth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_stroke_width)
        
        // Initialize views
        val previewView = findViewById<View>(R.id.stroke_preview)
        val widthValueText = findViewById<TextView>(R.id.width_value_text)
        val seekBar = findViewById<SeekBar>(R.id.stroke_width_seekbar)
        val btnThin = findViewById<TextView>(R.id.btn_thin)
        val btnMedium = findViewById<TextView>(R.id.btn_medium)
        val btnThick = findViewById<TextView>(R.id.btn_thick)
        val btnCancel = findViewById<TextView>(R.id.btn_cancel)
        val btnApply = findViewById<TextView>(R.id.btn_apply)
        
        // Set initial values
        seekBar.progress = initialWidth.toInt()
        updatePreview(initialWidth)
        widthValueText.text = "Độ dày: ${initialWidth.toInt()}"
        
        // Set up seekbar listener
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedWidth = progress.toFloat().coerceAtLeast(1f)
                updatePreview(selectedWidth)
                widthValueText.text = "Độ dày: ${selectedWidth.toInt()}"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Set up preset buttons
        btnThin.setOnClickListener {
            selectedWidth = 2f
            seekBar.progress = selectedWidth.toInt()
            updatePreview(selectedWidth)
            widthValueText.text = "Độ dày: ${selectedWidth.toInt()}"
        }
        
        btnMedium.setOnClickListener {
            selectedWidth = 10f
            seekBar.progress = selectedWidth.toInt()
            updatePreview(selectedWidth)
            widthValueText.text = "Độ dày: ${selectedWidth.toInt()}"
        }
        
        btnThick.setOnClickListener {
            selectedWidth = 20f
            seekBar.progress = selectedWidth.toInt()
            updatePreview(selectedWidth)
            widthValueText.text = "Độ dày: ${selectedWidth.toInt()}"
        }
        
        // Set up action buttons
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        btnApply.setOnClickListener {
            onStrokeWidthSelectedListener?.invoke(selectedWidth)
            dismiss()
        }
    }
    
    private fun updatePreview(width: Float) {
        val previewView = findViewById<View>(R.id.stroke_preview)
        val params = previewView.layoutParams
        params.height = width.toInt().coerceAtLeast(1)
        previewView.layoutParams = params
    }
    
    fun setOnStrokeWidthSelectedListener(listener: (Float) -> Unit) {
        onStrokeWidthSelectedListener = listener
    }
}