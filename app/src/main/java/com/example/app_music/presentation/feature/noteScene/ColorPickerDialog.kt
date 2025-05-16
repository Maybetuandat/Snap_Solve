package com.example.app_music.presentation.noteScene

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.example.app_music.R

class ColorPickerDialog(context: Context) : Dialog(context) {

    private var onColorSelectedListener: ((Int) -> Unit)? = null
    
    private lateinit var colorPreview: View
    private lateinit var redSeekBar: SeekBar
    private lateinit var greenSeekBar: SeekBar
    private lateinit var blueSeekBar: SeekBar
    private lateinit var redValueText: TextView
    private lateinit var greenValueText: TextView
    private lateinit var blueValueText: TextView
    
    private var red = 0
    private var green = 0
    private var blue = 0
    
    // Màu cơ bản
    private val predefinedColors = intArrayOf(
        Color.BLACK,
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.MAGENTA,
        Color.CYAN,
        Color.WHITE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)
        setContentView(view)
        
        // Gán các view
        colorPreview = findViewById(R.id.color_preview)
        redSeekBar = findViewById(R.id.seekbar_red)
        greenSeekBar = findViewById(R.id.seekbar_green)
        blueSeekBar = findViewById(R.id.seekbar_blue)
        redValueText = findViewById(R.id.text_red_value)
        greenValueText = findViewById(R.id.text_green_value)
        blueValueText = findViewById(R.id.text_blue_value)
        
        // Khởi tạo màu mặc định (đen)
        updateColor(0, 0, 0)
        
        // Tạo các nút màu cơ bản
        createColorButtons()
        
        // Thiết lập các seekbar
        setupSeekbars()
        
        // Nút OK để chọn màu
        findViewById<Button>(R.id.btn_ok).setOnClickListener {
            onColorSelectedListener?.invoke(Color.rgb(red, green, blue))
            dismiss()
        }
        
        // Nút Cancel để hủy
        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }
    }
    
    private fun createColorButtons() {
        val colorButtonsLayout = findViewById<LinearLayout>(R.id.color_buttons_layout)
        
        // Tạo các nút màu cơ bản
        for (color in predefinedColors) {
            val colorButton = View(context)
            val size = context.resources.getDimensionPixelSize(R.dimen.color_button_size)
            val layoutParams = LinearLayout.LayoutParams(size, size)
            layoutParams.setMargins(10, 0, 10, 0)
            
            colorButton.layoutParams = layoutParams
            colorButton.setBackgroundColor(color)
            
            // Thiết lập border cho nút màu
            if (color == Color.WHITE) {
                colorButton.setBackgroundResource(R.drawable.color_button_border)
            }
            
            // Sự kiện khi nhấn vào nút màu
            colorButton.setOnClickListener {
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                
                redSeekBar.progress = r
                greenSeekBar.progress = g
                blueSeekBar.progress = b
                
                updateColor(r, g, b)
            }
            
            colorButtonsLayout.addView(colorButton)
        }
    }
    
    private fun setupSeekbars() {
        // Thiết lập seeker cho màu đỏ
        redSeekBar.max = 255
        redSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                red = progress
                redValueText.text = progress.toString()
                updateColor(red, green, blue)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Thiết lập seeker cho màu xanh lá
        greenSeekBar.max = 255
        greenSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                green = progress
                greenValueText.text = progress.toString()
                updateColor(red, green, blue)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Thiết lập seeker cho màu xanh dương
        blueSeekBar.max = 255
        blueSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                blue = progress
                blueValueText.text = progress.toString()
                updateColor(red, green, blue)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun updateColor(r: Int, g: Int, b: Int) {
        red = r
        green = g
        blue = b
        
        // Cập nhật giá trị text
        redValueText.text = r.toString()
        greenValueText.text = g.toString()
        blueValueText.text = b.toString()
        
        // Cập nhật thanh seekbar
        redSeekBar.progress = r
        greenSeekBar.progress = g
        blueSeekBar.progress = b
        
        // Cập nhật màu preview
        colorPreview.setBackgroundColor(Color.rgb(r, g, b))
    }
    
    fun setOnColorSelectedListener(listener: (Int) -> Unit) {
        onColorSelectedListener = listener
    }
}