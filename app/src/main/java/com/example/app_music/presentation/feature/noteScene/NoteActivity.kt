package com.example.app_music.presentation.feature.noteScene

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_music.R
import com.example.app_music.data.model.FolderFirebaseModel
import com.example.app_music.data.model.NoteFirebaseModel
import com.example.app_music.data.repository.FirebaseNoteRepository
import com.example.app_music.databinding.ActivityNoteBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.common.SpacingItemDecoration
import com.example.app_music.presentation.feature.noteScene.model.NoteItem
import com.example.app_music.presentation.feature.noteScene.noteAdapter.NotesAdapter
import com.example.app_music.presentation.feature.qrscanner.QRScannerActivity
import com.example.app_music.utils.StorageManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NoteActivity : BaseActivity() {

    companion object {
        private const val TAG = "NoteActivity"
        private const val REQUEST_IMAGE_PICK = 100
        private const val REQUEST_IMAGE_CAPTURE = 101
        private const val PERMISSION_REQUEST_CODE = 200
    }

    private lateinit var binding: ActivityNoteBinding
    private lateinit var adapter: NotesAdapter
    private var notesList: MutableList<NoteItem> = mutableListOf()

    // Firebase repository
    private val repository = FirebaseNoteRepository()

    // Current user and folder info
    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Folder navigation
    private var currentFolderId: String? = null
    private var currentFolderTitle: String? = null
    private var isInFolder = false

    // Photo capture variables
    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null

    // Permission launcher for newer Android versions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (pendingAction == PendingAction.OPEN_CAMERA) {
                openCamera()
            } else if (pendingAction == PendingAction.OPEN_GALLERY) {
                openFilePicker()
            }
            pendingAction = PendingAction.NONE
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // For tracking pending actions after permission requests
    private enum class PendingAction {
        NONE, OPEN_CAMERA, OPEN_GALLERY
    }

    private var pendingAction = PendingAction.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        setupTypeButton()
        setupRecyclerView()
        setupMenuButton()

        // Check if we're opening a specific folder (from QR code or deep link)
        val folderId = intent.getStringExtra("folder_id")
        if (folderId != null) {
            lifecycleScope.launch {
                val folderResult = repository.getFolders().getOrNull()?.find { it.id == folderId }
                if (folderResult != null) {
                    openFolder(folderResult.id, folderResult.title, false)
                } else {
                    Toast.makeText(this@NoteActivity, "Folder not found", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Load folders by default (top level)
            loadFolders()
        }

        // Set up QR scanner button
        binding.fabScanQr.setOnClickListener {
            startActivity(Intent(this, QRScannerActivity::class.java))
        }
    }

    private fun setupBackButton() {
        binding.buttonBackNote.setOnClickListener {
            if (isInFolder) {
                // Go back to folders view
                showFoldersView()
            } else {
                // Standard back behavior
                onBackPressed()
            }
        }
    }

    private fun setupTypeButton() {
        binding.noteButtonType.setOnClickListener {
            val popupMenu = PopupMenu(this, it)
            popupMenu.menuInflater.inflate(R.menu.menu_note_type, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.type_day -> {
                        binding.noteButtonType.text = getString(R.string.day)
                        sortItemsByDate()
                        true
                    }
                    R.id.type_name -> {
                        binding.noteButtonType.text = getString(R.string.name)
                        sortItemsByName()
                        true
                    }
                    R.id.type_type -> {
                        binding.noteButtonType.text = getString(R.string.type)
                        sortItemsByType()
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        binding.recycleViewNote.layoutManager = layoutManager

        // Add item decoration for better spacing
        binding.recycleViewNote.addItemDecoration(
            SpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.item_spacing))
        )

        // Improve performance with fixed size
        binding.recycleViewNote.setHasFixedSize(true)

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
                // Open folder
                openFolder(folder.id, folder.title, true)
            }
        )
        binding.recycleViewNote.adapter = adapter
    }


    // Add a method to show permission rationale
    private fun showPermissionRationaleDialog(title: String, message: String, permission: String, action: PendingAction) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ ->
                pendingAction = action
                requestPermissionLauncher.launch(permission)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun setupMenuButton() {
        binding.noteButtonMenu.setOnClickListener {
            val menuRes = if (isInFolder) R.menu.menu_folder_action else R.menu.menu_note_action
            val popupMenu = PopupMenu(this, it)
            popupMenu.menuInflater.inflate(menuRes, popupMenu.menu)

            // Enable icons
            try {
                val menuField = PopupMenu::class.java.getDeclaredField("mPopup")
                menuField.isAccessible = true
                val menuPopupHelper = menuField.get(popupMenu)

                val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
                setForceIcons.invoke(menuPopupHelper, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->
                handleMenuItemClick(item.itemId)
            }

            popupMenu.show()
        }
    }

    private fun showFoldersView() {
        // Reset folder navigation state
        isInFolder = false
        currentFolderId = null
        currentFolderTitle = null

        // Update UI
        binding.textTitle.text = getString(R.string.noteTitle)
        updateMenuForFolderView()
        updatePathBar(null)
        // Load folders
        loadFolders()
    }

    private fun loadFolders() {
        binding.progressBar.visibility = View.VISIBLE
        notesList.clear()

        lifecycleScope.launch {
            try {
                val foldersResult = repository.getFolders()

                if (foldersResult.isSuccess) {
                    val folders = foldersResult.getOrNull() ?: emptyList()

                    // Convert to NoteItem objects
                    val folderItems = folders.map { folder ->
                        NoteItem(
                            id = folder.id,
                            title = folder.title,
                            date = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
                                .format(Date(folder.createdAt)),
                            isFolder = true
                        )
                    }

                    notesList.addAll(folderItems)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@NoteActivity, "Failed to load folders", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoteActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun openFolder(folderId: String, folderTitle: String, addToHistory: Boolean = false) {
        // Update state
        isInFolder = true
        currentFolderId = folderId
        currentFolderTitle = folderTitle

        // Update UI
        binding.textTitle.text = folderTitle
        updateMenuForNoteView()
        updatePathBar(folderTitle)
        // Load notes in this folder
        loadNotesInFolder(folderId)
    }

    private fun loadNotesInFolder(folderId: String) {
        binding.progressBar.visibility = View.VISIBLE
        notesList.clear()

        lifecycleScope.launch {
            try {
                val notesResult = repository.getNotes(folderId)

                if (notesResult.isSuccess) {
                    val notes = notesResult.getOrNull() ?: emptyList()

                    // Convert to NoteItem objects
                    val noteItems = notes.map { note ->
                        NoteItem(
                            id = note.id,
                            title = note.title,
                            date = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
                                .format(Date(note.createdAt)),
                            isFolder = false
                        )
                    }

                    notesList.addAll(noteItems)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@NoteActivity, "Failed to load notes", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoteActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateMenuForFolderView() {
        // In folder view (top level), only show folder creation option
        binding.noteButtonMenu.text = getString(R.string.create_new_folder)
    }

    private fun updateMenuForNoteView() {
        // In note view (inside folder), show note creation options
        binding.noteButtonMenu.text = getString(R.string.create_new_note)
    }

    private fun showNewItemOptions(anchorView: View?) {
        if (anchorView != null) {
            val popupMenu = if (isInFolder) {
                // Inside folder - show note creation options
                PopupMenu(this, anchorView).apply {
                    menuInflater.inflate(R.menu.menu_folder_action, menu)
                }
            } else {
                // Top level - show folder creation option
                PopupMenu(this, anchorView).apply {
                    menuInflater.inflate(R.menu.menu_note_action, menu)
                }
            }

            // Enable icons
            try {
                val menuField = PopupMenu::class.java.getDeclaredField("mPopup")
                menuField.isAccessible = true
                val menuPopupHelper = menuField.get(popupMenu)

                val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
                setForceIcons.invoke(menuPopupHelper, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->
                handleMenuItemClick(item.itemId)
            }

            popupMenu.show()
        } else {
            binding.noteButtonMenu.performClick()
        }
    }

    private fun showItemOptions(anchorView: View, item: NoteItem) {
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.menuInflater.inflate(R.menu.menu_item_options, popupMenu.menu)

        // Enable icons
        try {
            val menuField = PopupMenu::class.java.getDeclaredField("mPopup")
            menuField.isAccessible = true
            val menuPopupHelper = menuField.get(popupMenu)

            val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
            val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
            setForceIcons.invoke(menuPopupHelper, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Set color for delete option
        val deleteItem = popupMenu.menu.findItem(R.id.action_delete)
        val spanString = SpannableString(deleteItem.title.toString())
        spanString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.delete_red)),
            0, spanString.length, 0)
        deleteItem.title = spanString

        // Add share option for folders
        if (item.isFolder) {
            popupMenu.menu.add(0, R.id.action_share, 0, "Share").apply {
                setIcon(R.drawable.ic_help)
            }
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_rename -> {
                    showRenameDialog(item)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmation(item)
                    true
                }
                R.id.action_share -> {
                    showShareOptions(item)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showShareOptions(item: NoteItem) {
        val qrCodeFragment = QRCodeFragment.newInstance(item.id, item.isFolder)
        qrCodeFragment.show(supportFragmentManager, "qrcode_dialog")
    }

    private fun handleMenuItemClick(itemId: Int): Boolean {
        return when (itemId) {
            R.id.action_createfolder -> {
                showCreateFolderDialog()
                true
            }
            R.id.action_createnote, R.id.action_camera -> {
                if (isInFolder) {
                    checkCameraPermission()
                } else {
                    Toast.makeText(this, "Please enter a folder to create notes", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_uploadfile -> {
                if (isInFolder) {
                    checkStoragePermission()
                } else {
                    Toast.makeText(this, "Please enter a folder to upload notes", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_scan -> {
                if (isInFolder) {
                    // Start QR code scanner
                    startActivity(Intent(this, QRScannerActivity::class.java))
                } else {
                    Toast.makeText(this, "Please enter a folder to scan", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> false
        }
    }

    private fun showRenameDialog(item: NoteItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_name)
        editText.setText(item.title)

        AlertDialog.Builder(this)
            .setTitle(R.string.rename)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameItem(item, newName)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun renameItem(item: NoteItem, newName: String) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE

            try {
                val result = if (item.isFolder) {
                    // Rename folder
                    val folder = FolderFirebaseModel(
                        id = item.id,
                        title = newName,
                        ownerId = currentUserId,
                        // Keep other properties the same by getting current folder first
                        createdAt = Date().time,
                        updatedAt = Date().time
                    )
                    repository.updateFolder(folder)
                } else {
                    // Rename note - first get the current note
                    val noteResult = repository.getNote(item.id)

                    if (noteResult.isSuccess) {
                        val currentNote = noteResult.getOrNull()!!
                        val updatedNote = currentNote.copy(title = newName)
                        repository.updateNote(updatedNote)
                    } else {
                        Result.failure(Exception("Failed to get note"))
                    }
                }

                if (result.isSuccess) {
                    // Update local data
                    item.title = newName
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this@NoteActivity, "Renamed successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@NoteActivity, "Failed to rename", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoteActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showDeleteConfirmation(item: NoteItem) {
        val itemType = if (item.isFolder) "folder" else "note"

        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage("Are you sure you want to delete this $itemType? This action cannot be undone.")
            .setPositiveButton(android.R.string.yes) { _, _ ->
                deleteItem(item)
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }

    private fun deleteItem(item: NoteItem) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE

            try {
                val result = if (item.isFolder) {
                    repository.deleteFolder(item.id)
                } else {
                    repository.deleteNote(item.id)
                }

                if (result.isSuccess) {
                    // Remove from local list
                    notesList.remove(item)
                    adapter.notifyDataSetChanged()

                    val itemType = if (item.isFolder) "folder" else "note"
                    Toast.makeText(this@NoteActivity, "$itemType deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@NoteActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoteActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showCreateFolderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_name)
        editText.hint = "Enter folder name"

        AlertDialog.Builder(this)
            .setTitle("Create New Folder")
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    createNewFolder(folderName)
                } else {
                    Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun createNewFolder(folderName: String) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE

            try {
                val result = repository.createFolder(folderName)

                if (result.isSuccess) {
                    val newFolder = result.getOrNull()!!

                    // Add to local list
                    val folderItem = NoteItem(
                        id = newFolder.id,
                        title = newFolder.title,
                        date = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
                            .format(Date(newFolder.createdAt)),
                        isFolder = true
                    )

                    notesList.add(0, folderItem) // Add at the top
                    adapter.notifyDataSetChanged()

                    Toast.makeText(this@NoteActivity, "Folder created", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@NoteActivity, "Failed to create folder", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoteActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog(
                    "Camera Permission",
                    "Camera permission is needed to take photos for your notes",
                    Manifest.permission.CAMERA,
                    PendingAction.OPEN_CAMERA
                )
            }
            else -> {
                pendingAction = PendingAction.OPEN_CAMERA
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED) {
            pendingAction = PendingAction.OPEN_GALLERY
            requestPermissionLauncher.launch(permission)
        } else {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_IMAGE_PICK)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open file picker", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            try {
                val photoFile = createTempImageFile()
                currentPhotoPath = photoFile.absolutePath

                photoUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    photoFile
                )

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open camera", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePathBar(folderTitle: String?) {
        val pathView = binding.tvPath
        if (isInFolder && folderTitle != null) {
            pathView.text = "Home > $folderTitle"
            binding.pathBar.visibility = View.VISIBLE
        } else {
            pathView.text = "Home"
            binding.pathBar.visibility = View.GONE
        }
    }

    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    data?.data?.let { uri ->
                        photoUri = uri
                        showNoteNameDialog("Image_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}")
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    showNoteNameDialog("Camera_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}")
                }
            }
        }
    }

    private fun showNoteNameDialog(defaultName: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_name)
        editText.setText(defaultName)

        AlertDialog.Builder(this)
            .setTitle("Note Name")
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val noteName = editText.text.toString().trim()
                if (noteName.isNotEmpty()) {
                    createNote(noteName)
                } else {
                    Toast.makeText(this, "Note name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun createNote(noteName: String) {
        if (currentFolderId == null) {
            Toast.makeText(this, "Please enter a folder to create notes", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val contentResolver = applicationContext.contentResolver
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoUri)

                // First create the note in Firestore
                val result = repository.createNote(noteName, currentFolderId!!, null) // Pass null for now

                if (result.isSuccess) {
                    val newNote = result.getOrNull()!!

                    // Then upload the image to Firebase Storage
                    val storageManager = StorageManager(applicationContext)
                    val uploadSuccess = storageManager.saveImage(newNote.id, bitmap)

                    // Also save a thumbnail (scaled-down version)
                    val thumbnailBitmap = createThumbnail(bitmap)
                    val thumbnailSuccess = storageManager.saveThumbnail(newNote.id, thumbnailBitmap)

                    // Update the note with the image path
                    if (uploadSuccess) {
                        val imagePath = "${newNote.id}.jpg" // This is the path in Firebase Storage
                        val updatedNote = newNote.copy(imagePath = imagePath)
                        repository.updateNote(updatedNote)
                    }

                    // Add to local list
                    val noteItem = NoteItem(
                        id = newNote.id,
                        title = newNote.title,
                        date = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
                            .format(Date(newNote.createdAt)),
                        isFolder = false
                    )

                    notesList.add(0, noteItem) // Add at the top
                    adapter.notifyDataSetChanged()

                    Toast.makeText(this@NoteActivity, "Note created", Toast.LENGTH_SHORT).show()

                    // Open the note detail activity
                    val intent = Intent(this@NoteActivity, NoteDetailActivity::class.java).apply {
                        putExtra("note_id", newNote.id)
                        putExtra("note_title", newNote.title)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this@NoteActivity, "Failed to create note", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoteActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun createThumbnail(original: Bitmap): Bitmap {
        val width = original.width
        val height = original.height
        val maxSize = 200 // Thumbnail size

        val scale = Math.min(
            maxSize.toFloat() / width.toFloat(),
            maxSize.toFloat() / height.toFloat()
        )

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(original, 0, 0, width, height, matrix, true)
    }
    private fun sortItemsByDate() {
        notesList.sortByDescending {
            try {
                SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
                    .parse(it.date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun sortItemsByName() {
        notesList.sortBy { it.title.lowercase() }
        adapter.notifyDataSetChanged()
    }

    private fun sortItemsByType() {
        notesList.sortWith(compareBy({ !it.isFolder }, { it.title.lowercase() }))
        adapter.notifyDataSetChanged()
    }

    override fun onBackPressed() {
        if (isInFolder) {
            showFoldersView()
        } else {
            super.onBackPressed()
        }
    }
}