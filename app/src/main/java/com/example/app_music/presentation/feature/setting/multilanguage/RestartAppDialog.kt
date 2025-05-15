package com.example.app_music.presentation.feature.setting.multilanguage

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import com.example.app_music.R
import com.example.app_music.domain.utils.MultiLanguage

class RestartAppDialog(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_restart_app)


        window?.let {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(it.attributes)
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
            it.setBackgroundDrawableResource(android.R.color.transparent)
        }

        val btnRestart = findViewById<Button>(R.id.btnRestartNow)
        val btnLater = findViewById<Button>(R.id.btnRestartLater)

        btnRestart.setOnClickListener {
            dismiss()
            try {
                MultiLanguage.restartApp(context)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Không thể khởi động lại ứng dụng. Vui lòng khởi động lại thủ công.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        btnLater.setOnClickListener {
            dismiss()
        }
    }
}