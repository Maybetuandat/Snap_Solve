package com.example.app_music.presentation.noteScene

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.example.app_music.R

class NoteActivity : AppCompatActivity() {

    private lateinit var spinnerType: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        //spinner type
        spinnerType = findViewById(R.id.spinner_type_note)
        val itemType = listOf("Ngày", "Tên", "Loại")
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_note_item,
            R.id.spinner_text,
            itemType
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerType.adapter = adapter

        //spinner add

    }
}
