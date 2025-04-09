package com.example.app_music.presentation.noteScene

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import coil.ImageLoader
import com.example.app_music.R
import coil.load
import android.os.Build
import android.widget.Button
import android.widget.PopupMenu
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

class NoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        val btnType = findViewById<Button>(R.id.note_button_type)

        btnType.setOnClickListener {
            val popupMenu = PopupMenu(this, btnType) // yourButton là nút bạn bấm để mở menu
            popupMenu.menuInflater.inflate(R.menu.menu_note_type, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.type_day -> {
                        btnType.setText(R.string.day)
                        true
                    }
                    R.id.type_name -> {
                        btnType.setText(R.string.name)
                        true
                    }
                    R.id.type_type -> {
                        btnType.setText(R.string.type)
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()

        }

        //gif
        val imageView = findViewById<ImageView>(R.id.imageview_note)

        val imageLoader = ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

        val request = ImageRequest.Builder(this)
            .data(R.raw.pencils) // tên file không cần .gif
            .target(imageView)
            .build()

        imageLoader.enqueue(request)


        //new btn
        val btnMenu = findViewById<Button>(R.id.note_button_menu)

        btnMenu.setOnClickListener {
            val popupMenu = PopupMenu(this, btnMenu) // yourButton là nút bạn bấm để mở menu
            popupMenu.menuInflater.inflate(R.menu.menu_note_action, popupMenu.menu)

            try {
                val fields = popupMenu.javaClass.declaredFields
                for (field in fields) {
                    if (field.name == "mPopup") {
                        field.isAccessible = true
                        val menuPopupHelper = field.get(popupMenu)
                        val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                        val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_uploadfile -> { /* handle upload */ true }
                    R.id.action_createnote -> { /* handle create note */ true }
                    R.id.action_createfolder -> { /* handle folder */ true }
                    R.id.action_camera -> { /* handle camera */ true }
                    R.id.action_scan -> { /* handle scan */ true }
                    else -> false
                }
            }

            popupMenu.show()

        }

    }
}
