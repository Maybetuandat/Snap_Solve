package com.example.app_music.presentation.noteScene

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.app_music.R
import com.example.app_music.presentation.noteScene.model.NoteItem
import com.example.app_music.presentation.noteScene.noteAdapter.NotesAdapter

class NoteActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotesAdapter
    private var notesList: MutableList<NoteItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        // Set up type filter button
        setupTypeButton()

        // Load GIF animation
        loadGifAnimation()

        // Set up RecyclerView
        setupRecyclerView()

        // Set up menu button
        setupMenuButton()
    }

    private fun setupTypeButton() {
        val btnType = findViewById<Button>(R.id.note_button_type)

        btnType.setOnClickListener {
            val popupMenu = PopupMenu(this, btnType)
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
    }

    private fun loadGifAnimation() {
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
            .data(R.raw.pencils)
            .target(imageView)
            .build()

        imageLoader.enqueue(request)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycleViewNote)
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupNotesList()

        adapter = NotesAdapter(this, notesList)
        recyclerView.adapter = adapter
    }

    private fun setupMenuButton() {
        val btnMenu = findViewById<Button>(R.id.note_button_menu)

        btnMenu.setOnClickListener {
            val popupMenu = PopupMenu(this, btnMenu)
            popupMenu.menuInflater.inflate(R.menu.menu_note_action, popupMenu.menu)

            enablePopupIcons(popupMenu)

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

    private fun enablePopupIcons(popupMenu: PopupMenu) {
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
    }

    private fun setupNotesList() {
        notesList.clear()

        // Add a regular note
        notesList.add(NoteItem("1", "Ghi chú mới", "5 thg 4, 2025"))

        // Add an image note (without actual image for now)
        notesList.add(NoteItem("2", "Image_202503", "31 thg 3, 2025"))

        // Create a folder with nested notes
        val folder = NoteItem("3", "Lớp 9", "31 thg 3, 2025", true)

        // Add child notes to the folder
        folder.addChildNote(NoteItem("4", "Bài tập toán", "28 thg 3, 2025"))
        folder.addChildNote(NoteItem("5", "Bài tập văn", "29 thg 3, 2025"))
        folder.addChildNote(NoteItem("6", "Ghi chú khoa học", "30 thg 3, 2025"))

        // Add a nested folder inside the main folder
        val nestedFolder = NoteItem("7", "Tiếng Anh", "27 thg 3, 2025", true)
        nestedFolder.addChildNote(NoteItem("8", "Từ vựng", "26 thg 3, 2025"))
        nestedFolder.addChildNote(NoteItem("9", "Ngữ pháp", "25 thg 3, 2025"))

        folder.addChildNote(nestedFolder)

        // Add the folder to the main list
        notesList.add(folder)
    }
}