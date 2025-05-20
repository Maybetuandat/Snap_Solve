package com.example.app_music.presentation.feature.noteScene

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_music.MainActivity
import com.example.app_music.R
import com.example.app_music.data.model.FolderFirebaseModel
import com.example.app_music.data.model.NoteFirebaseModel
import com.example.app_music.data.model.NotePage
import com.example.app_music.data.repository.FirebaseNoteRepository
import com.example.app_music.databinding.ActivityNoteBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.common.SpacingItemDecoration
import com.example.app_music.presentation.feature.noteScene.model.NoteItem
import com.example.app_music.presentation.feature.noteScene.noteAdapter.NotesAdapter
import com.example.app_music.presentation.feature.qrscanner.QRScannerActivity
import com.example.app_music.utils.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // Current user info
    private val currentUserId: String
        get() = "test_user_1"

    // Folder navigation
    private var currentFolderId: String? = null
    private var currentFolderTitle: String? = null
    private var isInFolder = false

    // Photo capture variables
    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null

    // For tracking pending actions after permission requests
    private enum class PendingAction {
        NONE, OPEN_CAMERA, OPEN_GALLERY
    }

    private var pendingAction = PendingAction.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //set click cho back button
        setupBackButton()

        // set type button
        setupTypeButton()

        // set recycle view
        setupRecyclerView()

        //set button menu
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
                showFoldersView()
            } else {
                val intent = Intent(this, MainActivity::class.java)
                // Thêm flag để xóa các activity khác và không tạo instance mới của MainActivity
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                // Thêm extra để MainActivity biết chuyển đến HomeFragment
                intent.putExtra("SELECT_HOME_TAB", true)
                startActivity(intent)
                finish()
            }
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        navigateToHome()
    }
    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        // Thêm flag để xóa các activity khác và không tạo instance mới của MainActivity
        //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // Thêm extra để MainActivity biết chuyển đến HomeFragment
        intent.putExtra("SELECT_HOME_TAB", 100)
        startActivity(intent)
        finish()
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
            lifecycleScope = lifecycleScope,
            onNewItemClick = { anchorView ->
                showNewItemOptions(anchorView)
            },
            onItemOptionsClick = { anchorView, item ->
                showItemOptions(anchorView, item)
            },
            onFolderClick = { folder ->
                openFolder(folder.id, folder.title, true)
            }
        )
        binding.recycleViewNote.adapter = adapter
    }

    private fun setupMenuButton() {
        binding.noteButtonMenu.setOnClickListener {
            val menuRes = if (isInFolder) R.menu.menu_folder_action else R.menu.menu_note_action
            val popupMenu = PopupMenu(this, it)
            popupMenu.menuInflater.inflate(menuRes, popupMenu.menu)

            //enable icon
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
        isInFolder = false
        currentFolderId = null
        currentFolderTitle = null

        //cap nhat text la QANDA Note
        binding.textTitle.text = getString(R.string.noteTitle)

        updateMenuForFolderView()
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
                    Toast.makeText(this@NoteActivity, "Failed to load folders: " + foldersResult.isSuccess, Toast.LENGTH_SHORT).show()
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
        // Load notes in this folder
        loadNotesInFolder(folderId)
    }

    private fun loadNotesInFolder(folderId: String) {
        binding.progressBar.visibility = View.VISIBLE

        // Keep track of currently displayed notes to animate updates
        val currentNoteIds = notesList.map { it.id }

        lifecycleScope.launch {
            try {
                val notesResult = repository.getNotes(folderId)

                if (notesResult.isSuccess) {
                    val notes = notesResult.getOrNull() ?: emptyList()

                    // Create new note items with fresh data
                    val updatedNotes = notes.map { note ->
                        NoteItem(
                            id = note.id,
                            title = note.title,
                            date = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
                                .format(Date(note.createdAt)),
                            isFolder = false
                        )
                    }

                    // Clear existing cached images to force reload
                    // This is important when returning from NoteDetailActivity
                    val storageManager = StorageManager(applicationContext);
                    for (note in updatedNotes) {
                        storageManager.clearImageCache(note.id)
                    }

                    // Update the list with new data
                    withContext(Dispatchers.Main) {
                        notesList.clear()
                        notesList.addAll(updatedNotes)
                        adapter.notifyDataSetChanged()
                    }
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
        binding.noteButtonMenu.text = getString(R.string.newbutton)
    }

    private fun updateMenuForNoteView() {
        binding.noteButtonMenu.text = getString(R.string.newbutton)
    }

    private fun showNewItemOptions(anchorView: View?) {
        if (anchorView != null) {
            val popupMenu = if (isInFolder) {
                PopupMenu(this, anchorView).apply {
                    menuInflater.inflate(R.menu.menu_folder_action, menu)
                }
            } else {
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
            R.id.action_createnote -> {
                if (isInFolder) {
                    createBlankNote() // Hàm mới để tạo ghi chú trắng
                } else {
                    Toast.makeText(this, "Vui lòng chọn một thư mục trước", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_uploadfile -> {
                if (isInFolder) {
                    checkStoragePermission() // Mở gallery để chọn ảnh
                } else {
                    Toast.makeText(this, "Vui lòng chọn một thư mục trước", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_camera -> {
                if (isInFolder) {
                    checkCameraPermission() // Mở camera để chụp ảnh
                } else {
                    Toast.makeText(this, "Vui lòng chọn một thư mục trước", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> false
        }
    }

    private fun createBlankNote() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Tạo tên mặc định cho ghi chú
                val noteName = "Note_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"

                // Tạo note mới
                val noteId = UUID.randomUUID().toString()
                val note = NoteFirebaseModel(
                    id = noteId,
                    title = noteName,
                    createdAt = Date().time,
                    updatedAt = Date().time,
                    ownerId = currentUserId,
                    folderId = currentFolderId!!
                )

                // Tạo trang trắng cho note
                val pageId = UUID.randomUUID().toString()
                val page = NotePage(
                    id = pageId,
                    noteId = noteId,
                    pageIndex = 0,
                    createdAt = Date().time,
                    vectorDrawingData = """{"strokes":[],"width":800,"height":1200}"""
                )

                // Thêm pageId vào note
                note.pageIds.add(pageId)

                // Lưu note và page vào Firestore
                val pageResult = repository.updatePage(page)
                val noteResult = repository.createNoteWithId(note)

                if (noteResult.isSuccess && pageResult.isSuccess) {
                    // Tạo bitmap trắng làm thumbnail
                    val whiteBitmap = createWhiteBackground(200, 300)
                    val storageManager = StorageManager(applicationContext)
                    storageManager.saveThumbnail(noteId, whiteBitmap)

                    // Thêm vào danh sách hiển thị
                    val noteItem = NoteItem(
                        id = noteId,
                        title = noteName,
                        date = SimpleDateFormat("d MMM, yyyy", Locale.getDefault()).format(Date()),
                        isFolder = false,
                        imagePreview = whiteBitmap
                    )

                    withContext(Dispatchers.Main) {
                        notesList.add(0, noteItem)
                        adapter.notifyDataSetChanged()

                        Toast.makeText(this@NoteActivity, "Đã tạo ghi chú trắng", Toast.LENGTH_SHORT).show()

                        // Mở note detail
                        val intent = Intent(this@NoteActivity, NoteDetailActivity::class.java).apply {
                            putExtra("note_id", noteId)
                            putExtra("note_title", noteName)
                        }
                        startActivity(intent)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@NoteActivity, "Không thể tạo ghi chú", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NoteActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun createWhiteBackground(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        return bitmap
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

    // Phương thức createNewFolder cần sửa như sau:
    private fun createNewFolder(folderName: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Kiểm tra xác thực
                if (currentUserId.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@NoteActivity, "Cần đăng nhập để tạo thư mục", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                val result = repository.createFolder(folderName)

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val newFolder = result.getOrNull()!!

                        val folderItem = NoteItem(
                            id = newFolder.id,
                            title = newFolder.title,
                            date = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
                                .format(Date(newFolder.createdAt)),
                            isFolder = true
                        )

                        // Thêm và cập nhật cụ thể
                        notesList.add(0, folderItem)
                        adapter.notifyItemInserted(0)

                        Toast.makeText(this@NoteActivity, "Đã tạo thư mục", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@NoteActivity,
                            "Không thể tạo thư mục: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NoteActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                // Yêu cầu NHIỀU quyền cùng lúc, đảm bảo cả CAMERA và STORAGE đều được cấp
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun checkStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        } else {
            openFilePicker()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allPermissionsGranted) {
                // Đã có tất cả quyền cần thiết, tiếp tục hành động
                when (pendingAction) {
                    PendingAction.OPEN_CAMERA -> openCamera()
                    PendingAction.OPEN_GALLERY -> openFilePicker()
                    else -> { /* Không làm gì */ }
                }
            } else {
                // Người dùng từ chối một hoặc nhiều quyền
                Toast.makeText(
                    this,
                    "Ứng dụng cần tất cả quyền để hoạt động đúng",
                    Toast.LENGTH_LONG
                ).show()
            }
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

    // Tìm phương thức createNote() trong NoteActivity.kt và sửa như sau:
    private fun createNote(noteName: String) {
        if (currentFolderId == null) {
            Toast.makeText(this, "Vui lòng vào một thư mục để tạo ghi chú", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val contentResolver = applicationContext.contentResolver

                // Kiểm tra photoUri
                if (photoUri == null) {
                    Toast.makeText(this@NoteActivity, "Không có ảnh để tạo note", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }

                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
                Log.d("NoteActivity", "Bitmap tạo được: ${bitmap.width}x${bitmap.height}")

                // Tạo ID cho note
                val noteId = UUID.randomUUID().toString()

                // Upload ảnh trước khi tạo note
                val storageManager = StorageManager(applicationContext)
                val imagePath = "${noteId}.jpg"

                // Upload ảnh lên Storage
                val uploadSuccess = storageManager.saveImage(noteId, bitmap)
                Log.d("NoteActivity", "Upload ảnh thành công: $uploadSuccess")

                // Upload thumbnail
                val thumbnailBitmap = createThumbnail(bitmap)
                val thumbnailSuccess = storageManager.saveThumbnail(noteId, thumbnailBitmap)
                Log.d("NoteActivity", "Upload thumbnail thành công: $thumbnailSuccess")

                if (uploadSuccess) {
                    // Tạo note với imagePath
                    val newNote = NoteFirebaseModel(
                        id = noteId,
                        title = noteName,
                        createdAt = Date().time,
                        updatedAt = Date().time,
                        ownerId = currentUserId,
                        folderId = currentFolderId!!,
                        imagePath = imagePath
                    )

                    // QUAN TRỌNG: Thay đổi ở đây - KHÔNG tạo page trống mặc định
                    // Cần tạo page với ảnh ngay từ đầu thay vì tạo note trước

                    // Tạo một pageId mới
                    val pageId = UUID.randomUUID().toString()

                    // Tạo page chứa ảnh
                    val page = NotePage(
                        id = pageId,
                        noteId = noteId,
                        pageIndex = 0,
                        imagePath = "pages/${pageId}.jpg",  // Xác định đường dẫn ảnh rõ ràng
                        createdAt = Date().time
                    )

                    // Lưu ảnh cho page
                    storageManager.savePageImage(pageId, bitmap)

                    // Lưu page vào Firestore
                    repository.updatePage(page)

                    // Thêm pageId vào danh sách pageIds của note
                    newNote.pageIds.add(pageId)

                    // Lưu note với danh sách pageIds đã cập nhật
                    val result = repository.createNoteWithId(newNote)

                    if (result.isSuccess) {
                        // Thêm vào danh sách cục bộ
                        val noteItem = NoteItem(
                            id = noteId,
                            title = noteName,
                            date = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
                                .format(Date()),
                            isFolder = false,
                            imagePreview = thumbnailBitmap
                        )

                        notesList.add(0, noteItem)
                        adapter.notifyDataSetChanged()

                        Toast.makeText(this@NoteActivity, "Đã tạo ghi chú", Toast.LENGTH_SHORT).show()

                        // Mở note detail
                        val intent = Intent(this@NoteActivity, NoteDetailActivity::class.java).apply {
                            putExtra("note_id", noteId)
                            putExtra("note_title", noteName)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@NoteActivity, "Không thể tạo ghi chú", Toast.LENGTH_SHORT).show()
                        Log.e("NoteActivity", "Lỗi tạo ghi chú: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    Toast.makeText(this@NoteActivity, "Không thể tải lên ảnh", Toast.LENGTH_SHORT).show()
                    Log.e("NoteActivity", "Không thể tải lên ảnh")
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoteActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("NoteActivity", "Lỗi tạo ghi chú", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun createThumbnail(original: Bitmap): Bitmap {
        try {
            val width = original.width
            val height = original.height
            val maxSize = 200 // Thumbnail size

            val scale = Math.min(
                maxSize.toFloat() / width.toFloat(),
                maxSize.toFloat() / height.toFloat()
            )

            val matrix = Matrix()
            matrix.postScale(scale, scale)


            val result = Bitmap.createBitmap(original, 0, 0, width, height, matrix, true)
            Log.d("NoteActivity", "Thumbnail created successfully: ${result.width}x${result.height}")
            return result
        } catch (e: Exception) {
            Log.e("NoteActivity", "Error creating thumbnail: ${e.message}")
            // Trả về bitmap gốc nếu có lỗi
            return original
        }
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

    override fun onResume() {
        super.onResume()

        if (isInFolder && currentFolderId != null) {
            loadNotesInFolder(currentFolderId!!)
        }
    }
}