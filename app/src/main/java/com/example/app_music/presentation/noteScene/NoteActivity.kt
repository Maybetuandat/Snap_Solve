package com.example.app_music.presentation.noteScene

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
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
    private var mainNotesList: MutableList<NoteItem> = mutableListOf()

    // UI elements
    private lateinit var titleTextView: TextView
    private lateinit var backButton: ImageButton
    private lateinit var addNewButton: Button

    // Tracking current folder
    private var currentFolderId: String? = null
    private var currentFolderTitle: String? = null
    private var isInFolder = false
    private var folderHistory = mutableListOf<FolderHistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        // Initialize UI elements
        titleTextView = findViewById(R.id.text_title)
        backButton = findViewById(R.id.button_back_note)
        addNewButton = findViewById(R.id.note_button_menu)

        // Set up back button
        setupBackButton()

        // Set up type filter button
        setupTypeButton()

        // Load GIF animation
        loadGifAnimation()

        // Set up RecyclerView
        setupRecyclerView()

        // Set up menu button
        setupMenuButton()
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            if (isInFolder) {
                // Go back to previous folder or main screen
                navigateBack()
            } else {
                // Standard back behavior when on main screen
                onBackPressed()
            }
        }
    }

    private fun navigateBack() {
        if (folderHistory.isEmpty()) {
            // If history is empty, go back to main notes list
            showMainNotesList()
        } else {
            // Pop the last folder from history
            val previousFolder = folderHistory.removeAt(folderHistory.size - 1)

            if (previousFolder.folderId == null) {
                // If previous was the main screen
                showMainNotesList()
            } else {
                // Navigate to previous folder
                previousFolder.folderId?.let { folderId ->
                    previousFolder.folderTitle?.let { folderTitle ->
                        openFolder(folderId, folderTitle, false)
                    }
                }
            }
        }
    }

    private fun showMainNotesList() {
        // Reset state
        isInFolder = false
        currentFolderId = null
        currentFolderTitle = null
        folderHistory.clear()

        // Update UI
        titleTextView.text = getString(R.string.noteTitle)

        // Use original menu with folder creation option
        addNewButton.setOnClickListener {
            val popupMenu = PopupMenu(this, addNewButton)
            popupMenu.menuInflater.inflate(R.menu.menu_note_action, popupMenu.menu)
            enablePopupIcons(popupMenu)
            popupMenu.setOnMenuItemClickListener { item ->
                handleMenuItemClick(item.itemId)
            }
            popupMenu.show()
        }

        // Restore original notes list
        notesList.clear()
        notesList.addAll(mainNotesList)
        adapter.notifyDataSetChanged()
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

        adapter = NotesAdapter(
            context = this,
            notesList = notesList,
            onNewItemClick = { anchorView ->
                showNewItemOptions(anchorView)
            },
            onItemOptionsClick = { anchorView, item ->
                showItemOptions(anchorView, item)
            },
            onFolderClick = { folder ->
                // Open folder contents
                openFolder(folder.id, folder.title, true)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun openFolder(folderId: String, folderTitle: String, addToHistory: Boolean) {
        // Save current state to history if needed
        if (addToHistory) {
            folderHistory.add(FolderHistoryItem(currentFolderId, currentFolderTitle))
        }

        // Update current folder info
        isInFolder = true
        currentFolderId = folderId
        currentFolderTitle = folderTitle

        // Update UI
        titleTextView.text = folderTitle

        // Change menu to folder menu (without folder creation)
        addNewButton.setOnClickListener {
            val popupMenu = PopupMenu(this, addNewButton)
            popupMenu.menuInflater.inflate(R.menu.menu_folder_action, popupMenu.menu)
            enablePopupIcons(popupMenu)
            popupMenu.setOnMenuItemClickListener { item ->
                handleMenuItemClick(item.itemId)
            }
            popupMenu.show()
        }

        // Load folder contents
        loadFolderNotes(folderId)
    }

    private fun loadFolderNotes(folderId: String) {
        // Clear current list
        notesList.clear()

        // Add folder contents based on ID
        when (folderId) {
            "3" -> { // Lớp 9
                notesList.add(NoteItem("4", "Bài tập toán", "28 thg 3, 2025"))
                notesList.add(NoteItem("5", "Bài tập văn", "29 thg 3, 2025"))
                notesList.add(NoteItem("6", "Ghi chú khoa học", "30 thg 3, 2025"))
                notesList.add(NoteItem("7", "Tiếng Anh", "27 thg 3, 2025", true)) // Folder con
            }
            "7" -> { // Tiếng Anh
                notesList.add(NoteItem("8", "Từ vựng", "26 thg 3, 2025"))
                notesList.add(NoteItem("9", "Ngữ pháp", "25 thg 3, 2025"))
            }
            else -> {
                // Unknown folder
                Toast.makeText(this, "Không tìm thấy dữ liệu folder", Toast.LENGTH_SHORT).show()
            }
        }

        // Notify adapter
        adapter.notifyDataSetChanged()
    }

    private fun showNewItemOptions(anchorView: View?) {
        if (anchorView != null) {
            // Show menu right below the clicked view
            val popupMenu = if (isInFolder) {
                PopupMenu(this, anchorView).apply {
                    menuInflater.inflate(R.menu.menu_folder_action, menu)
                }
            } else {
                PopupMenu(this, anchorView).apply {
                    menuInflater.inflate(R.menu.menu_note_action, menu)
                }
            }

            enablePopupIcons(popupMenu)

            popupMenu.setOnMenuItemClickListener { item ->
                handleMenuItemClick(item.itemId)
            }

            popupMenu.show()
        } else {
            // If no view was passed, use the default menu button
            addNewButton.performClick()
        }
    }

    private fun showItemOptions(anchorView: View, item: NoteItem) {
        // Show options menu for item (note or folder)
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.menuInflater.inflate(R.menu.menu_item_options, popupMenu.menu)

        enablePopupIcons(popupMenu)

        // Set red color for Delete text
        try {
            val menu = popupMenu.menu
            val deleteItem = menu.findItem(R.id.action_delete)
            deleteItem?.let {
                // Create a SpannableString to set color for text
                val spannableString = SpannableString(it.title)
                spannableString.setSpan(ForegroundColorSpan(getColor(R.color.delete_red)), 0, spannableString.length, 0)
                it.title = spannableString
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_rename -> {
                    // Show rename dialog
                    showRenameDialog(item)
                    true
                }
                R.id.action_delete -> {
                    // Show delete confirmation
                    showDeleteConfirmation(item)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun handleMenuItemClick(itemId: Int): Boolean {
        return when (itemId) {
            R.id.action_uploadfile -> {
                Toast.makeText(this, "Upload file selected", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_createnote -> {
                Toast.makeText(this, "Create note selected", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_createfolder -> {
                Toast.makeText(this, "Create folder selected", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_camera -> {
                Toast.makeText(this, "Camera selected", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_scan -> {
                Toast.makeText(this, "Scan selected", Toast.LENGTH_SHORT).show()
                true
            }
            else -> false
        }
    }

    private fun setupMenuButton() {
        addNewButton.setOnClickListener {
            val menuRes = if (isInFolder) R.menu.menu_folder_action else R.menu.menu_note_action
            val popupMenu = PopupMenu(this, addNewButton)
            popupMenu.menuInflater.inflate(menuRes, popupMenu.menu)

            enablePopupIcons(popupMenu)

            popupMenu.setOnMenuItemClickListener { item ->
                handleMenuItemClick(item.itemId)
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

    private fun showRenameDialog(item: NoteItem) {
        // Create dialog to rename
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_name)
        editText.setText(item.title)

        AlertDialog.Builder(this)
            .setTitle(R.string.rename)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    // Update name
                    renameItem(item, newName)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun renameItem(item: NoteItem, newName: String) {
        // Update item name
        item.title = newName
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Renamed to: $newName", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmation(item: NoteItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delete)
        builder.setMessage("Are you sure you want to delete ${item.title}?")

        // Set red color for Delete button
        builder.setPositiveButton(android.R.string.yes) { _, _ ->
            deleteItem(item)
        }
        builder.setNegativeButton(android.R.string.no, null)

        val dialog = builder.create()
        dialog.show()

        // Set red color for positive button (Delete)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.delete_red))
    }

    private fun deleteItem(item: NoteItem) {
        // Remove item from list
        notesList.remove(item)
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "${item.title} deleted", Toast.LENGTH_SHORT).show()
    }

    private fun setupNotesList() {
        notesList.clear()
        mainNotesList.clear()

        // Add notes to both lists
        val regularNote = NoteItem("1", "Ghi chú mới", "5 thg 4, 2025")
        notesList.add(regularNote)
        mainNotesList.add(regularNote)

        // Add image note
        val imageNote = NoteItem("2", "Image_202503", "31 thg 3, 2025")
        notesList.add(imageNote)
        mainNotesList.add(imageNote)

        // Create folder with nested notes
        val folder = NoteItem("3", "Lớp 9", "31 thg 3, 2025", true)

        // Add child notes to folder
        folder.addChildNote(NoteItem("4", "Bài tập toán", "28 thg 3, 2025"))
        folder.addChildNote(NoteItem("5", "Bài tập văn", "29 thg 3, 2025"))
        folder.addChildNote(NoteItem("6", "Ghi chú khoa học", "30 thg 3, 2025"))

        // Add nested folder
        val nestedFolder = NoteItem("7", "Tiếng Anh", "27 thg 3, 2025", true)
        nestedFolder.addChildNote(NoteItem("8", "Từ vựng", "26 thg 3, 2025"))
        nestedFolder.addChildNote(NoteItem("9", "Ngữ pháp", "25 thg 3, 2025"))

        folder.addChildNote(nestedFolder)

        // Add folder to lists
        notesList.add(folder)
        mainNotesList.add(folder)
    }

    // Helper class to store folder navigation history
    data class FolderHistoryItem(val folderId: String?, val folderTitle: String?)

    // Handle back button
    override fun onBackPressed() {
        if (isInFolder) {
            navigateBack()
        } else {
            super.onBackPressed()
        }
    }
}