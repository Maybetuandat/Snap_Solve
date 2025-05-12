package com.example.app_music.presentation.feature.setting.restartappdialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import com.example.app_music.R
import com.example.app_music.domain.utils.MultiLanguage

class RestartAppDialog(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_restart_app)

        val tvMessage = findViewById<TextView>(R.id.tvRestartMessage)
        val btnRestart = findViewById<Button>(R.id.btnRestartNow)
        val btnLater = findViewById<Button>(R.id.btnRestartLater)

        btnRestart.setOnClickListener {
            dismiss()
            MultiLanguage.restartApp(context)
        }

        btnLater.setOnClickListener {
            dismiss()
        }
    }
}