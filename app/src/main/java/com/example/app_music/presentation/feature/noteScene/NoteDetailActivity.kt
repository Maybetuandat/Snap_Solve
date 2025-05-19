package com.example.app_music.presentation.feature.noteScene

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.app_music.R
import com.example.app_music.data.collaboration.CollaborationManager
import com.example.app_music.data.model.NotePage
import com.example.app_music.data.repository.FirebaseNoteRepository
import com.example.app_music.databinding.ActivityNoteDetailBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.noteScene.views.DrawingView
import com.example.app_music.presentation.noteScene.ColorPickerDialog
import com.example.app_music.utils.StorageManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.UUID
import android.Manifest
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.MotionEvent
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.app_music.presentation.feature.noteScene.views.RealtimeDrawingManager
import com.google.gson.Gson
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit

class NoteDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var repository: FirebaseNoteRepository
    private lateinit var collaborationManager: CollaborationManager
    private lateinit var storageManager: StorageManager

    private var noteId: String = ""
    private var noteTitle: String = ""
    private var fromQrCode: Boolean = false
    private val CAMERA_PERMISSION_CODE = 1001
    private val STORAGE_PERMISSION_CODE = 1002
    // Drawing variables
    private var currentColor = Color.BLACK
    private var currentWidth = 5f

    // User colors for collaboration
    private val userColors = arrayOf(
        Color.RED,
        Color.BLUE,
        Color.GREEN,
        Color.MAGENTA,
        Color.CYAN,
        Color.DKGRAY
    )
    private val userColor = userColors[Random().nextInt(userColors.size)]

    // Firebase Auth
    private val auth = FirebaseAuth.getInstance()
    private val currentUser get() = auth.currentUser
    private var saveJob: Job? = null
    private val AUTO_SAVE_DELAY = 2000L
    // Pagination variables
    private var currentPageIndex = 0
    private var pages = mutableListOf<NotePage>()
    private var currentPage: NotePage? = null
    private val lastEditTimes = mutableMapOf<String, Long>()
    // Photo capture variables
    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var tempPhotoUri: Uri? = null
    private var tempPhotoPath: String? = null
    private var drawingManager: RealtimeDrawingManager? = null
    // Activity result launchers
    private val takePictureContract = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            // Use the temp photo URI that's set just before launching
            addImagePage(tempPhotoUri!!)
            // Clear the temporary URI after use
            tempPhotoUri = null
        }
    }

    private val pickImageContract = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            addImagePage(uri)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, take picture
                    takePicture()
                } else {
                    Toast.makeText(this, "Camera permission required to take photos", Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, pick image
                    pickImage()
                } else {
                    Toast.makeText(this, "Storage permission required to select images", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase repository and storage manager
        repository = FirebaseNoteRepository()
        storageManager = StorageManager(applicationContext)

        // Set canvas background to light gray for better visibility of blank pages
        binding.canvasContainer.setBackgroundColor(Color.LTGRAY)

        // Initialize collaboration manager
        collaborationManager = CollaborationManager(noteId)

        // Set up UI
        setupToolbar()
        setupDrawingTools()
        setupHelpButtons()
        setupPagination()

        // Handle deep links or regular intent extras
        if (intent.data != null) {
            Log.d("NoteDetailActivity", "Opening from deep link: ${intent.data}")
            handleDeepLink(intent)
        } else {
            // Regular flow - get intent extras
            noteId = intent.getStringExtra("note_id") ?: ""
            noteTitle = intent.getStringExtra("note_title") ?: ""
            fromQrCode = intent.getBooleanExtra("from_qr_code", false)

            Log.d("NoteDetailActivity", "Opening note with ID: $noteId, title: $noteTitle")

            if (noteId.isEmpty()) {
                Toast.makeText(this, "Note ID is missing", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Load note data - only once!
            loadNote()
        }
        startAutoSave()
        // Set up collaboration
        setupCollaboration()
        binding.drawingView.resetTransform()
        setupPageEventListener()
        binding.drawingView.setOnDrawCompletedListener(object : DrawingView.OnDrawCompletedListener {
            override fun onDrawCompleted() {
                // Cập nhật UI
                updateUndoRedoButtons()

                // Cập nhật thời gian chỉnh sửa cuối cùng
                currentPage?.let { page ->
                    lastEditTimes[page.id] = System.currentTimeMillis()
                }

                // Kiểm tra nếu cần lưu
                if (binding.drawingView.isForceSaveNeeded()) {
                    debouncedSaveCurrentPage()
                }
            }
        })

        // Thêm vào cuối phương thức onCreate
        startAutoSave()
    }


    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = noteTitle
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupDrawingTools() {
        // Set default mode
        selectMode(DrawingView.DrawMode.DRAW)

        // Tool selection buttons
        binding.buttonHand.setOnClickListener {
            selectMode(DrawingView.DrawMode.PAN)
        }

        binding.buttonPen.setOnClickListener {
            selectMode(DrawingView.DrawMode.DRAW)
        }

        binding.buttonEraser.setOnClickListener {
            selectMode(DrawingView.DrawMode.ERASE)
        }

        // Undo/redo buttons with explicit saving
        binding.buttonUndo.setOnClickListener {
            val undoSuccess = binding.drawingView.undo()
            updateUndoRedoButtons()
            if (undoSuccess) {
                // Save immediately after undo
                saveCurrentPage(false)
            }
        }

        binding.buttonRedo.setOnClickListener {
            val redoSuccess = binding.drawingView.redo()
            updateUndoRedoButtons()
            if (redoSuccess) {
                // Save immediately after redo
                saveCurrentPage(false)
            }
        }

        // Set draw completed listener with forced save check
        binding.drawingView.setOnDrawCompletedListener(object : DrawingView.OnDrawCompletedListener {
            override fun onDrawCompleted() {
                // Update UI
                updateUndoRedoButtons()

                // Check if force save is needed
                if (binding.drawingView.isForceSaveNeeded()) {
                    saveCurrentPage(false)
                }
            }
        })
    }

    private fun showStrokeOptionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Tùy chọn nét vẽ")
            .setItems(arrayOf("Xóa nét vẽ")) { _, which ->
                when (which) {
                    0 -> {
                        binding.drawingView.deleteSelectedStroke()
                        saveCurrentPage(false) // Lưu sau khi xóa
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }


    // Add this method to handle page deletion:
    private fun showDeletePageConfirmation() {
        if (pages.size <= 1) {
            Toast.makeText(this, "Cannot delete the only page", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Page")
            .setMessage("Are you sure you want to delete this page? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePage()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePage() {
        if (currentPage == null || pages.isEmpty()) {
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val pageToDelete = currentPage!!
                val deleteIndex = currentPageIndex
                val result = repository.deletePage(pageToDelete.id)

                if (result.isSuccess) {
                    // Remove from local list
                    pages.removeAt(deleteIndex)

                    // Update page indices for all pages after the deleted page
                    val updatedIndices = mutableListOf<NotePage>()
                    for (i in deleteIndex until pages.size) {
                        val updatedPage = pages[i].copy(pageIndex = i)
                        pages[i] = updatedPage
                        updatedIndices.add(updatedPage)
                    }

                    // Update all indices in one batch if possible
                    updatedIndices.forEach { page ->
                        repository.updatePage(page)
                    }

                    // Update note's pageIds list
                    val noteResult = repository.getNote(pageToDelete.noteId)
                    if (noteResult.isSuccess) {
                        val note = noteResult.getOrNull()!!
                        note.pageIds.remove(pageToDelete.id)
                        repository.updateNote(note)

                        // Notify all collaborators of the page deletion
                        collaborationManager.emitPageEvent(
                            CollaborationManager.PageEventType.PAGE_DELETED,
                            note.id,
                            pageToDelete.id,
                            deleteIndex,
                            note.pageIds
                        )
                    }

                    Toast.makeText(this@NoteDetailActivity, "Page deleted", Toast.LENGTH_SHORT).show()

                    // Navigate to appropriate page
                    if (pages.isEmpty()) {
                        // If no pages left, create a blank one
                        val blankPageResult = repository.createBlankPage(pageToDelete.noteId)
                        if (blankPageResult.isSuccess) {
                            val page = blankPageResult.getOrNull()!!
                            pages.add(page)
                            currentPageIndex = 0

                            // Notify collaborators of new page
                            noteResult.getOrNull()?.let { note ->
                                note.pageIds.add(page.id)
                                repository.updateNote(note)

                                collaborationManager.emitPageEvent(
                                    CollaborationManager.PageEventType.PAGE_DELETED,
                                    note.id,
                                    pageToDelete.id,
                                    deleteIndex,
                                    note.pageIds
                                )
                            }
                        }
                    } else {
                        // Navigate to previous page, or next if this was the first
                        currentPageIndex = if (deleteIndex > 0) deleteIndex - 1 else 0
                    }

                    // Load the new current page
                    loadPage(currentPageIndex)
                } else {
                    Toast.makeText(this@NoteDetailActivity,
                        "Error deleting page: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error deleting page", e)
                Toast.makeText(this@NoteDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupPagination() {
        // Navigation buttons
        binding.buttonPrevPage.setOnClickListener {
            navigateToPreviousPage()
        }

        binding.buttonNextPage.setOnClickListener {
            navigateToNextPage()
        }

        binding.buttonAddPage.setOnClickListener {
            showAddPageDialog()
        }

        binding.fabAddPage.setOnClickListener {
            showAddPageDialog()
        }

        // Initially disable navigation buttons until pages are loaded
        updateNavigationButtons()
    }

    private fun setupHelpButtons() {
        binding.buttonHelpAi.setOnClickListener {
            Toast.makeText(this, "AI help feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.buttonViewExplanation.setOnClickListener {
            Toast.makeText(this, "Explanation feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    private fun debouncedSaveCurrentPage() {
        // Cancel previous save job if it exists
        saveJob?.cancel()

        // Create a new save job with delay
        saveJob = lifecycleScope.launch {
            delay(AUTO_SAVE_DELAY)
            saveCurrentPage(false) // Save without toast
        }
    }
    private var previousUserCount = 0
    private fun setupCollaboration() {
        // Mark user as active
        val username = currentUser?.displayName ?: "Anonymous"
        collaborationManager.setUserPresence(username, userColor)

        // Observe active users with improved UI feedback
        lifecycleScope.launch {
            collaborationManager.getActiveUsers().collectLatest { users ->
                binding.activeUsersView.updateActiveUsers(users)
                binding.userCount.text = "${users.size}"

                // Show toast when a new user joins (except for the first user, which is the current user)
                if (previousUserCount > 0 && users.size > previousUserCount) {
                    val newUser = users.lastOrNull { it.userId != currentUser?.uid }
                    if (newUser != null) {
                        Toast.makeText(this@NoteDetailActivity,
                            "${newUser.username} joined", Toast.LENGTH_SHORT).show()
                    }
                }
                previousUserCount = users.size
            }
        }
    }

    private fun syncDrawingAction() {
        binding.drawingView.getLastStroke()?.let { stroke ->
            val drawingAction = collaborationManager.strokeToDrawingAction(stroke)
            collaborationManager.saveDrawingAction(drawingAction)
        }
    }
    private fun saveDrawing(showToast: Boolean = true) {
        lifecycleScope.launch {
            try {
                binding.saveProgressBar.visibility = View.VISIBLE

                // Lấy note hiện tại
                val noteResult = repository.getNote(noteId)
                if (noteResult.isSuccess) {
                    val note = noteResult.getOrNull()!!

                    // Lưu dữ liệu vector mới
                    val vectorJson = binding.drawingView.getDrawingDataAsJson()

                    // Lưu cả bitmap để tương thích ngược (nếu cần)
                    val bitmap = binding.drawingView.getCombinedBitmap()
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    val base64Drawing = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT)

                    // Cập nhật note
                    val updatedNote = note.copy(
                        vectorDrawingData = vectorJson,
                        drawingData = base64Drawing,
                        updatedAt = System.currentTimeMillis()
                    )

                    // Lưu vào Firestore
                    repository.updateNote(updatedNote)

                    if (showToast) {
                        Toast.makeText(this@NoteDetailActivity, "Đã lưu bản vẽ", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Lỗi lưu bản vẽ: ${e.message}")
                if (showToast) {
                    Toast.makeText(this@NoteDetailActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                binding.saveProgressBar.visibility = View.GONE
            }
        }
    }
    private fun loadNote() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Fetch note data
                val noteResult = repository.getNote(noteId)

                if (noteResult.isSuccess) {
                    val note = noteResult.getOrNull()!!

                    // Update title if needed
                    if (noteTitle.isEmpty()) {
                        noteTitle = note.title
                        supportActionBar?.title = noteTitle
                    }

                    // Load pages if they exist
                    if (note.pageIds.isEmpty()) {
                        // If no pages exist, create a first page
                        Log.d("NoteDetailActivity", "No pages exist, creating first page")
                        val blankPageResult = repository.createBlankPage(noteId)
                        if (blankPageResult.isSuccess) {
                            val page = blankPageResult.getOrNull()!!
                            pages.add(page)
                        } else {
                            Toast.makeText(this@NoteDetailActivity,
                                "Failed to create first page", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Load existing pages
                        Log.d("NoteDetailActivity", "Loading ${note.pageIds.size} existing pages")
                        val pagesResult = repository.getPages(noteId)
                        if (pagesResult.isSuccess) {
                            pages.clear()
                            pages.addAll(pagesResult.getOrNull()!!)

                            // Sort pages by index
                            pages.sortBy { it.pageIndex }
                            Log.d("NoteDetailActivity", "Loaded ${pages.size} pages")
                        } else {
                            Log.e("NoteDetailActivity", "Failed to load pages: ${pagesResult.exceptionOrNull()}")
                        }
                    }

                    // Load the first page
                    if (pages.isNotEmpty()) {
                        currentPageIndex = 0
                        loadPage(currentPageIndex)
                    }

                    // Update navigation buttons
                    updateNavigationButtons()
                    updatePageIndicator()

                } else {
                    // Handle note fetch failure
                    Toast.makeText(this@NoteDetailActivity,
                        "Could not load note: ${noteResult.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT).show()
                    Log.e("NoteDetailActivity", "Failed to fetch note: ${noteResult.exceptionOrNull()}")
                }
                if (noteResult.isSuccess) {
                    val note = noteResult.getOrNull()!!

                    // Ưu tiên tải dữ liệu vector nếu có
                    if (!note.vectorDrawingData.isNullOrEmpty()) {
                        try {
                            binding.drawingView.setDrawingDataFromJson(note.vectorDrawingData!!)
                            Log.d("NoteDetailActivity", "Đã tải dữ liệu vẽ dạng vector")
                        } catch (e: Exception) {
                            Log.e("NoteDetailActivity", "Lỗi khi tải dữ liệu vector: ${e.message}")

                            // Nếu tải vector thất bại, thử tải bitmap cũ
                            note.drawingData?.let { loadBitmapDrawingData(it) }
                        }
                    } else if (note.drawingData != null) {
                        // Nếu không có dữ liệu vector, tải bitmap cũ
                        loadBitmapDrawingData(note.drawingData!!)
                    }
                }
            } catch (e: Exception) {
                // Handle any other exceptions
                Toast.makeText(this@NoteDetailActivity,
                    "Error loading note: ${e.message}",
                    Toast.LENGTH_SHORT).show()
                Log.e("NoteDetailActivity", "Error in loadNote", e)
            } finally {
                // Always hide progress indicator
                binding.progressBar.visibility = View.GONE
            }
        }

    }

    // Update the loadPage method to handle image loading better and set a gray background
    // Phương thức hỗ trợ để tải dữ liệu vẽ dạng bitmap
    private fun loadBitmapDrawingData(base64Data: String) {
        try {
            val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            if (bitmap != null) {
                // Đặt bitmap làm ảnh nền để vẽ tiếp lên trên
                binding.drawingView.setBackgroundImage(bitmap)
                Log.d("NoteDetailActivity", "Đã tải dữ liệu vẽ dạng bitmap")
            } else {
                Log.e("NoteDetailActivity", "Không thể giải mã dữ liệu bitmap")
            }
        } catch (e: Exception) {
            Log.e("NoteDetailActivity", "Lỗi khi tải dữ liệu bitmap: ${e.message}")
        }
    }
    private fun loadPage(index: Int) {
        if (pages.isEmpty()) {
            // Clear everything if no pages
            binding.drawingView.clearDrawing(false)
            binding.imageNote.setImageDrawable(null)
            binding.imageNote.visibility = View.GONE
            binding.canvasContainer.setBackgroundColor(Color.LTGRAY)
            updatePageIndicator()
            updateNavigationButtons()
            return
        }

        if (index < 0 || index >= pages.size) {
            Log.e("NoteDetailActivity", "Invalid page index: $index")
            return
        }

        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE

        // Quan trọng: Lưu trang hiện tại trước khi chuyển - với cơ chế đồng bộ
        // sử dụng runBlocking để đảm bảo hoàn tất lưu trước khi tiếp tục
        currentPage?.let {
            runBlocking {
                try {
                    val job = lifecycleScope.launch {
                        saveCurrentPage(false)
                    }
                    // Đợi việc lưu hoàn tất nhưng có timeout để tránh bị treo
                    withTimeout(3000) {
                        job.join()
                    }
                } catch (e: Exception) {
                    Log.e("NoteDetailActivity", "Error saving current page before navigation", e)
                    // Vẫn tiếp tục chuyển trang ngay cả khi lưu lỗi
                }
            }
        }

        // Reset trang hiện tại
        clearPageContent() // Xóa sạch nội dung của trang

        // Cập nhật trang hiện tại
        currentPage = pages[index]
        currentPageIndex = index

        // Tải trang mới
        lifecycleScope.launch {
            try {
                val page = pages[index]
                Log.d("NoteDetailActivity", "Loading page ${page.id}")

                // Đầu tiên xử lý background
                var backgroundLoaded = false
                if (page.imagePath != null) {
                    try {
                        // Thử tải ảnh
                        val bitmap = storageManager.loadPageImage(page.id)
                        if (bitmap != null) {
                            binding.drawingView.setBackgroundImage(bitmap)
                            backgroundLoaded = true
                        } else {
                            // Thử tải từ URL
                            val imageResult = repository.getImageBitmap(page.imagePath!!)
                            if (imageResult.isSuccess) {
                                try {
                                    // Sử dụng Glide để tải ảnh từ URL
                                    Glide.with(this@NoteDetailActivity)
                                        .asBitmap()
                                        .load(imageResult.getOrNull())
                                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Thêm cache
                                        .into(object : CustomTarget<Bitmap>() {
                                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                                binding.drawingView.setBackgroundImage(resource)
                                            }

                                            override fun onLoadCleared(placeholder: Drawable?) {}
                                        })

                                    backgroundLoaded = true
                                } catch (e: Exception) {
                                    Log.e("NoteDetailActivity", "Error loading image with Glide", e)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("NoteDetailActivity", "Error loading image", e)
                    }
                }

                // Tạo nền trắng nếu không có ảnh
                if (!backgroundLoaded) {
                    binding.drawingView.setWhiteBackground(800, 1200)
                }

                // Đợi một chút để background tải xong
                delay(100)

                // Khởi tạo real-time drawing - điều này cũng sẽ tải các nét vẽ
                drawingManager?.cleanup() // Đảm bảo dọn dẹp trước
                drawingManager = RealtimeDrawingManager(
                    binding.drawingView,
                    collaborationManager,
                    lifecycleScope,
                    page.id
                )

                // Cập nhật UI
                updatePageIndicator()
                updateNavigationButtons()
                updateUndoRedoButtons()

            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error loading page", e)
                Toast.makeText(this@NoteDetailActivity,
                    "Error loading page: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    private fun clearPageContent() {
        Log.d("NoteDetailActivity", "Clearing all page content")

        // Stop drawing manager first
        drawingManager?.clearForPageChange()
        drawingManager = null  // Important: Set to null to prevent further events

        // Reset transformations BEFORE clearing drawing
        binding.drawingView.resetTransform()

        // Clear completely without triggering events to maintain proper state
        binding.drawingView.clearDrawing(false)

        // Xóa ảnh background
        binding.drawingView.setBackgroundImage(null)

        // Xóa và ẩn ImageView
        binding.imageNote.setImageDrawable(null)
        binding.imageNote.visibility = View.GONE

        // Reset container background
        binding.canvasContainer.setBackgroundColor(Color.LTGRAY)
    }
    private suspend fun loadPageImage(page: NotePage) {
        try {
            // First try loading from storage
            val bitmap = storageManager.loadPageImage(page.id)

            if (bitmap != null) {
                // Successfully loaded from storage
                Log.d("NoteDetailActivity", "Loaded page image from storage: ${bitmap.width}x${bitmap.height}")

                // Set as drawing view background
                withContext(Dispatchers.Main) {
                    binding.drawingView.setBackgroundImage(bitmap)
                }
                return
            }

            // If we're here, storage loading failed - try from URL
            if (page.imagePath != null) {
                val imageResult = repository.getImageBitmap(page.imagePath!!)

                if (imageResult.isSuccess) {
                    val uri = imageResult.getOrNull()

                    try {
                        // Use Glide with a timeout to load the image
                        val future = Glide.with(this@NoteDetailActivity)
                            .asBitmap()
                            .load(uri)
                            .submit()

                        val loadedBitmap = withTimeout(5000) { future.get() }

                        // Set as drawing view background
                        withContext(Dispatchers.Main) {
                            binding.drawingView.setBackgroundImage(loadedBitmap)
                        }

                        return
                    } catch (e: Exception) {
                        Log.e("NoteDetailActivity", "Error loading bitmap with Glide", e)

                        // Fallback to ImageView
                        withContext(Dispatchers.Main) {
                            binding.imageNote.visibility = View.VISIBLE
                            Glide.with(this@NoteDetailActivity)
                                .load(uri)
                                .into(binding.imageNote)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NoteDetailActivity", "Error loading page image", e)

            // If all attempts fail, fallback to white background
            createWhiteBackground()
        }
    }
    private fun createWhiteBackground() {
        binding.drawingView.setWhiteBackground(800, 1200)
        binding.imageNote.visibility = View.GONE
    }

    private suspend fun loadPageDrawings(page: NotePage) {
        withContext(Dispatchers.Main) {
            // Always clear drawing first
            binding.drawingView.clearDrawing(false)
        }

        if (page.vectorDrawingData.isNullOrEmpty()) {
            Log.d("NoteDetailActivity", "No drawing data for page ${page.id}")
            return
        }

        try {
            Log.d("NoteDetailActivity", "Loading vector drawing data for page ${page.id}")

            // Set the drawing data
            withContext(Dispatchers.Main) {
                // Try to parse the JSON data first to validate it
                try {
                    val gson = com.google.gson.Gson()
                    val drawingData = gson.fromJson(page.vectorDrawingData, binding.drawingView.getDrawingDataClass())

                    if (drawingData != null) {
                        binding.drawingView.setDrawingData(drawingData)
                        Log.d("NoteDetailActivity", "Successfully loaded drawing data with ${drawingData.strokes.size} strokes")
                    } else {
                        Log.e("NoteDetailActivity", "Invalid drawing data (null)")
                    }
                } catch (e: Exception) {
                    Log.e("NoteDetailActivity", "Error parsing drawing data JSON", e)
                }
            }
        } catch (e: Exception) {
            Log.e("NoteDetailActivity", "Error loading vector drawing data", e)
        }
    }

    // New helper method to initialize real-time drawing
    private fun initializeRealtimeDrawing(page: NotePage) {
        drawingManager = RealtimeDrawingManager(
            binding.drawingView,
            collaborationManager,
            lifecycleScope,
            page.id
        )
    }
    private fun setupPageEventListener() {
        lifecycleScope.launch {
            collaborationManager.getPageEventsFlow().collectLatest { event ->
                when (event.getEventType()) { // Use the helper method instead of direct enum
                    CollaborationManager.PageEventType.PAGE_DELETED -> {
                        // Handle page deletion from another user
                        handleRemotePageDeletion(event.pageId, event.allPageIds)
                    }
                    CollaborationManager.PageEventType.PAGES_REORDERED -> {
                        // Handle page reordering from another user
                        handleRemotePageReordering(event.allPageIds)
                    }
                    CollaborationManager.PageEventType.PAGE_ADDED -> {
                        // Handle new page addition from another user
                        handleRemotePageAddition(event.pageId, event.pageIndex)
                    }
                }
            }
        }
    }
    private fun handleRemotePageReordering(newPageOrdering: List<String>) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // Save current pageId to maintain position
                val currentPageId = currentPage?.id

                // Get the new pages in the correct order
                val newOrderedPages = mutableListOf<NotePage>()

                for (pageId in newPageOrdering) {
                    // Find the page in our existing list
                    val existingPage = pages.find { it.id == pageId }

                    if (existingPage != null) {
                        // Use the existing page data but with updated index
                        newOrderedPages.add(existingPage)
                    } else {
                        // Fetch the page from repository if we don't have it
                        val pageResult = repository.getPage(pageId)
                        if (pageResult.isSuccess) {
                            newOrderedPages.add(pageResult.getOrNull()!!)
                        }
                    }
                }

                // Update our page list with the new order
                if (newOrderedPages.isNotEmpty()) {
                    // Update indices based on new order
                    for (i in newOrderedPages.indices) {
                        newOrderedPages[i] = newOrderedPages[i].copy(pageIndex = i)
                    }

                    // Replace our page list
                    pages.clear()
                    pages.addAll(newOrderedPages)

                    // Find current page in new ordering
                    val newIndex = if (currentPageId != null) {
                        pages.indexOfFirst { it.id == currentPageId }.takeIf { it >= 0 } ?: 0
                    } else {
                        0
                    }

                    // Update current page index
                    currentPageIndex = newIndex

                    // Update UI
                    updatePageIndicator()
                    updateNavigationButtons()

                    Toast.makeText(this@NoteDetailActivity,
                        "Page ordering updated by a collaborator", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error handling remote page reordering", e)
            }
        }
    }

    // Handle remote page addition
    private fun handleRemotePageAddition(pageId: String, pageIndex: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // Try to get the new page from the repository
                val pageResult = repository.getPage(pageId)

                if (pageResult.isSuccess) {
                    val newPage = pageResult.getOrNull()!!

                    // Check if we already have this page
                    if (pages.none { it.id == pageId }) {
                        // Insert the page at the correct index
                        if (pageIndex >= 0 && pageIndex <= pages.size) {
                            pages.add(pageIndex, newPage)

                            // Update indices of all pages
                            for (i in 0 until pages.size) {
                                if (pages[i].pageIndex != i) {
                                    pages[i] = pages[i].copy(pageIndex = i)
                                }
                            }

                            // Update current page index if needed
                            if (pageIndex <= currentPageIndex) {
                                currentPageIndex++
                            }

                            // Update UI
                            updatePageIndicator()
                            updateNavigationButtons()

                            Toast.makeText(this@NoteDetailActivity,
                                "New page added by a collaborator", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error handling remote page addition", e)
            }
        }
    }
    private fun handleRemotePageDeletion(deletedPageId: String, newPageOrdering: List<String>) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // Remove page from local list
                val deletedIndex = pages.indexOfFirst { it.id == deletedPageId }
                if (deletedIndex >= 0) {
                    pages.removeAt(deletedIndex)

                    // Update current page index if needed
                    if (currentPageIndex >= pages.size) {
                        currentPageIndex = maxOf(0, pages.size - 1)
                    } else if (currentPageIndex > deletedIndex) {
                        // Adjust index if we were past the deleted page
                        currentPageIndex--
                    }

                    // If current page was deleted, load the new current page
                    if (currentPage?.id == deletedPageId) {
                        if (pages.isNotEmpty()) {
                            loadPage(currentPageIndex)
                        } else {
                            // No pages left, create a placeholder view
                            binding.drawingView.clearDrawing()
                            binding.imageNote.setImageDrawable(null)
                        }
                    }

                    // Update page indicators and navigation buttons
                    updatePageIndicator()
                    updateNavigationButtons()

                    Toast.makeText(this@NoteDetailActivity,
                        "A collaborator deleted a page", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error handling remote page deletion", e)
            }
        }
    }

    private fun saveCurrentPage(showToast: Boolean = true) {
        if (currentPage == null) return

        lifecycleScope.launch {
            try {
                binding.saveProgressBar.visibility = View.VISIBLE

                // Cập nhật timestamp cho trang
                val updatedPage = currentPage!!.copy(
                    createdAt = System.currentTimeMillis()
                )

                // Đánh dấu thời gian này để biết lần cập nhật cuối cùng
                val saveTimestamp = System.currentTimeMillis()

                // Lưu trang vào repository
                val result = repository.updatePage(updatedPage)

                if (result.isSuccess) {
                    // Cập nhật bản sao cục bộ
                    currentPage = result.getOrNull()
                    pages[currentPageIndex] = currentPage!!

                    // Thông báo cho người dùng nếu cần
                    if (showToast) {
                        Toast.makeText(this@NoteDetailActivity, "Page saved", Toast.LENGTH_SHORT).show()
                    }

                    Log.d("NoteDetailActivity", "Page saved successfully at $saveTimestamp")
                } else {
                    Log.e("NoteDetailActivity", "Error saving page: ${result.exceptionOrNull()}")

                    if (showToast) {
                        Toast.makeText(this@NoteDetailActivity,
                            "Error saving page: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error in saveCurrentPage", e)

                if (showToast) {
                    Toast.makeText(this@NoteDetailActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            } finally {
                binding.saveProgressBar.visibility = View.GONE
            }
        }
    }


    // Add an automatic save timer
    private var autoSaveJob: Job? = null
    private val AUTO_SAVE_INTERVAL = 10000L // 10 giây

    private fun startAutoSave() {
        // Hủy job cũ nếu có
        autoSaveJob?.cancel()

        // Khởi tạo job mới
        autoSaveJob = lifecycleScope.launch {
            try {
                while (isActive) {
                    delay(AUTO_SAVE_INTERVAL)

                    // Kiểm tra nếu có trang hiện tại
                    currentPage?.let { page ->
                        // Kiểm tra xem trang có bị chỉnh sửa kể từ lần lưu cuối không
                        val lastDrawingTime = binding.drawingView.getLastEditTime() ?: 0L
                        val lastSaveTime = lastEditTimes[page.id] ?: 0L

                        if (lastDrawingTime > lastSaveTime) {
                            // Có chỉnh sửa mới - thực hiện lưu
                            Log.d("NoteDetailActivity", "Auto-saving page ${page.id}")
                            saveCurrentPage(false)

                            // Cập nhật thời gian lưu cuối cùng
                            lastEditTimes[page.id] = System.currentTimeMillis()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error in auto-save job", e)
                // Restart auto-save job if it fails
                startAutoSave()
            }
        }
    }

    private fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }

    private fun navigateToPreviousPage() {
        if (currentPageIndex > 0 && pages.isNotEmpty()) {
            loadPage(currentPageIndex - 1)
        }
    }

    private fun navigateToNextPage() {
        if (currentPageIndex < pages.size - 1 && pages.isNotEmpty()) {
            loadPage(currentPageIndex + 1)
        }
    }

    private fun updateNavigationButtons() {
        // Enable/disable prev button
        binding.buttonPrevPage.isEnabled = currentPageIndex > 0
        binding.buttonPrevPage.alpha = if (currentPageIndex > 0) 1.0f else 0.5f

        // Enable/disable next button
        binding.buttonNextPage.isEnabled = currentPageIndex < pages.size - 1
        binding.buttonNextPage.alpha = if (currentPageIndex < pages.size - 1) 1.0f else 0.5f
    }

    private fun updatePageIndicator() {
        if (pages.isEmpty()) {
            binding.textPageIndicator.text = "No pages"
        } else {
            binding.textPageIndicator.text = "Page ${currentPageIndex + 1} of ${pages.size}"
        }
    }


    private fun showAddPageDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_page, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set up click listeners
        dialogView.findViewById<View>(R.id.layout_blank_page).setOnClickListener {
            addBlankPage()
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.layout_camera_page).setOnClickListener {
            takePicture()
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.layout_gallery_page).setOnClickListener {
            pickImage()
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.button_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addBlankPage() {
        binding.progressBar.visibility = View.VISIBLE

        Log.d("NoteDetailActivity", "Adding a new blank page")
        Toast.makeText(this, "Creating blank page...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // Create a new blank page
                val pageResult = repository.createBlankPage(noteId)

                if (pageResult.isSuccess) {
                    val newPage = pageResult.getOrNull()!!
                    Log.d("NoteDetailActivity", "Successfully created blank page with ID: ${newPage.id}")

                    // Add to pages list
                    pages.add(newPage)

                    // Force reload of UI elements
                    updatePageIndicator()
                    updateNavigationButtons()

                    // Navigate to the new page
                    val newIndex = pages.size - 1
                    Log.d("NoteDetailActivity", "Navigating to new page at index $newIndex")
                    loadPage(newIndex)
                    binding.drawingView.resetTransform()
                    Toast.makeText(this@NoteDetailActivity,
                        "Blank page added", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("NoteDetailActivity", "Failed to add blank page: ${pageResult.exceptionOrNull()}")
                    Toast.makeText(this@NoteDetailActivity,
                        "Failed to add blank page", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error adding blank page", e)
                Toast.makeText(this@NoteDetailActivity,
                    "Error adding blank page: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    // Fix for the takePicture method

    private fun takePicture() {
        // Check for camera permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                CAMERA_PERMISSION_CODE
            )
            return
        }

        try {
            // Create a file in app-specific directory that's definitely covered by FileProvider
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"

            // Use the cache directory instead, which is typically covered by default
            val storageDir = cacheDir // Use internal cache directory, not external
            val photoFile = File.createTempFile(imageFileName, ".jpg", storageDir)

            // Log the file path for debugging
            Log.d("NoteDetailActivity", "Created temp file at: ${photoFile.absolutePath}")

            // Set the temp path
            tempPhotoPath = photoFile.absolutePath

            // Create URI using FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )

            // Log the URI for debugging
            Log.d("NoteDetailActivity", "Created URI: $uri")

            // Store the URI and launch camera
            tempPhotoUri = uri
            takePictureContract.launch(uri)

        } catch (e: Exception) {
            Log.e("NoteDetailActivity", "Error taking picture: ${e.message}", e)
            e.printStackTrace() // Print full stack trace for debugging
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()

            // Clear variables on error
            tempPhotoUri = null
            tempPhotoPath = null
        }
    }


    private fun pickImage() {
        // Check for storage permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+, we need READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    STORAGE_PERMISSION_CODE
                )
                return
            }
        } else {
            // For older versions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
                return
            }
        }

        try {
            // Launch gallery picker
            pickImageContract.launch("image/*")
        } catch (e: Exception) {
            Log.e("NoteDetailActivity", "Error picking image: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addImagePage(imageUri: Uri) {
        binding.progressBar.visibility = View.VISIBLE

        Log.d("NoteDetailActivity", "Adding a new image page from URI: $imageUri")
        Toast.makeText(this, "Creating image page...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // First get bitmap from URI
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        if (tempPhotoPath != null && tempPhotoUri != null &&
                            imageUri.toString() == tempPhotoUri.toString()) {
                            // If this is from camera, load from file path
                            Log.d("NoteDetailActivity", "Loading image from file: $tempPhotoPath")
                            BitmapFactory.decodeFile(tempPhotoPath)
                        } else {
                            // If from gallery, load from content URI
                            Log.d("NoteDetailActivity", "Loading image from content URI")
                            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                        }
                    } catch (e: Exception) {
                        Log.e("NoteDetailActivity", "Error decoding bitmap: ${e.message}", e)
                        null
                    }
                }

                if (bitmap == null) {
                    Toast.makeText(this@NoteDetailActivity,
                        "Failed to load image", Toast.LENGTH_SHORT).show()
                    Log.e("NoteDetailActivity", "Bitmap is null - failed to load image")
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }

                Log.d("NoteDetailActivity", "Image loaded successfully: ${bitmap.width}x${bitmap.height}")

                // Create a new page
                val pageId = UUID.randomUUID().toString()

                // Save image to storage
                Log.d("NoteDetailActivity", "Saving image to storage with page ID: $pageId")
                val imageSaved = storageManager.savePageImage(pageId, bitmap)

                if (!imageSaved) {
                    Toast.makeText(this@NoteDetailActivity,
                        "Failed to save image", Toast.LENGTH_SHORT).show()
                    Log.e("NoteDetailActivity", "Failed to save image to storage")
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }

                // Create the page in database
                Log.d("NoteDetailActivity", "Creating page in database")
                val page = NotePage(
                    id = pageId,
                    noteId = noteId,
                    pageIndex = pages.size,
                    imagePath = "pages/$pageId.jpg",
                    createdAt = Date().time
                )

                val pageResult = repository.updatePage(page)

                if (pageResult.isSuccess) {
                    Log.d("NoteDetailActivity", "Page created successfully in database")

                    // Update note's page list
                    val noteResult = repository.getNote(noteId)
                    if (noteResult.isSuccess) {
                        val note = noteResult.getOrNull()!!
                        note.pageIds.add(pageId)
                        repository.updateNote(note)
                        Log.d("NoteDetailActivity", "Note updated with new page ID")
                    }

                    // Add to pages list
                    pages.add(page)

                    // Force reload of UI elements
                    updatePageIndicator()
                    updateNavigationButtons()

                    // Navigate to the new page
                    val newIndex = pages.size - 1
                    Log.d("NoteDetailActivity", "Navigating to new image page at index $newIndex")
                    loadPage(newIndex)

                    Toast.makeText(this@NoteDetailActivity,
                        "Image page added", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("NoteDetailActivity", "Failed to create page in database: ${pageResult.exceptionOrNull()}")
                    Toast.makeText(this@NoteDetailActivity,
                        "Failed to add image page", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error adding image page", e)
                e.printStackTrace()
                Toast.makeText(this@NoteDetailActivity,
                    "Error adding image page: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                // Clear the variables
                tempPhotoPath = null
                tempPhotoUri = null
            }
        }
    }
    private fun selectMode(mode: DrawingView.DrawMode) {
        // Set the drawing mode
        binding.drawingView.setMode(mode)

        // Update UI to highlight selected tool
        binding.buttonHand.setBackgroundResource(
            if (mode == DrawingView.DrawMode.PAN) R.drawable.tool_selected_bg
            else android.R.color.transparent
        )
        binding.buttonPen.setBackgroundResource(
            if (mode == DrawingView.DrawMode.DRAW) R.drawable.tool_selected_bg
            else android.R.color.transparent
        )
        binding.buttonEraser.setBackgroundResource(
            if (mode == DrawingView.DrawMode.ERASE) R.drawable.tool_selected_bg
            else android.R.color.transparent
        )

        // Configure drawing view based on mode
        when (mode) {
            DrawingView.DrawMode.DRAW -> {
                binding.drawingView.setColor(currentColor)
                binding.drawingView.setStrokeWidth(currentWidth)
            }
            DrawingView.DrawMode.ERASE -> {
                binding.drawingView.setStrokeWidth(currentWidth * 2) // Tẩy rộng hơn
            }
            DrawingView.DrawMode.PAN -> {
                // No special configuration needed
            }
            else -> {
                // Xử lý các chế độ khác nếu có
            }
        }
    }

    private fun showColorPicker() {
        val colorPickerDialog = ColorPickerDialog(this)
        colorPickerDialog.setOnColorSelectedListener { color ->
            currentColor = color
            binding.buttonColor.setColorFilter(color)

            if (binding.drawingView.getMode() == DrawingView.DrawMode.DRAW) {
                binding.drawingView.setColor(color)
            }
        }
        colorPickerDialog.show()
    }

    private fun showStrokeWidthDialog() {
        val dialog = StrokeWidthDialog(this, currentWidth)
        dialog.setOnStrokeWidthSelectedListener { width ->
            currentWidth = width

            // Update stroke width based on current mode
            if (binding.drawingView.getMode() == DrawingView.DrawMode.DRAW) {
                binding.drawingView.setStrokeWidth(width)
            } else if (binding.drawingView.getMode() == DrawingView.DrawMode.ERASE) {
                binding.drawingView.setStrokeWidth(width * 2)
            }
        }
        dialog.show()
    }

    private fun updateUndoRedoButtons() {
        val canUndo = binding.drawingView.canUndo()
        val canRedo = binding.drawingView.canRedo()

        Log.d("NoteDetailActivity", "Updating undo/redo buttons: undo=$canUndo, redo=$canRedo")

        binding.buttonUndo.isEnabled = canUndo
        binding.buttonUndo.alpha = if (canUndo) 1.0f else 0.5f

        binding.buttonRedo.isEnabled = canRedo
        binding.buttonRedo.alpha = if (canRedo) 1.0f else 0.5f
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Save before exiting
                saveCurrentPage(true)
                finish()
                true
            }
            R.id.action_share -> {
                showShareDialog()
                true
            }
            R.id.action_collaborate -> {
                showCollaborateDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun loadNoteWithSpecificPages(noteId: String, pageIds: List<String>) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // First get the note
                val noteResult = repository.getNote(noteId)

                if (noteResult.isSuccess) {
                    val note = noteResult.getOrNull()!!

                    // Update title
                    noteTitle = note.title
                    supportActionBar?.title = noteTitle

                    // Load only the specified pages
                    val allPages = mutableListOf<NotePage>()
                    for (pageId in pageIds) {
                        val pageResult = repository.getPage(pageId)
                        if (pageResult.isSuccess) {
                            val page = pageResult.getOrNull()!!
                            if (page.noteId == noteId) {
                                allPages.add(page)
                            }
                        }
                    }

                    // Sort pages by index
                    allPages.sortBy { it.pageIndex }

                    // Update pages list
                    pages.clear()
                    pages.addAll(allPages)

                    // Load the first page
                    if (pages.isNotEmpty()) {
                        currentPageIndex = 0
                        loadPage(currentPageIndex)
                    }

                    // Update navigation buttons
                    updateNavigationButtons()
                    updatePageIndicator()
                } else {
                    Toast.makeText(this@NoteDetailActivity,
                        "Could not load note: ${noteResult.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoteDetailActivity,
                    "Error loading note: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    private fun showShareDialog() {
        val qrCodeFragment = QRCodeFragment.newInstance(noteId, false)
        qrCodeFragment.show(supportFragmentManager, "qrcode_dialog")
    }
    private fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null) {
            // Parse the URI
            val path = data.path
            if (path != null) {
                if (path.startsWith("/notes/")) {
                    // Extract note ID
                    val noteId = path.removePrefix("/notes/")
                    Log.d("NoteDetailActivity", "Deep link for note ID: $noteId")

                    // Set the note ID
                    this.noteId = noteId

                    // Check if specific pages are requested
                    val pagesParam = data.getQueryParameter("pages")
                    if (pagesParam != null) {
                        // Load specific pages
                        val pageIds = pagesParam.split(",")
                        Log.d("NoteDetailActivity", "Loading specific pages: $pageIds")
                        loadNoteWithSpecificPages(noteId, pageIds)
                    } else {
                        // Load the full note
                        Log.d("NoteDetailActivity", "Loading full note")
                        loadNote()
                    }
                } else if (path.startsWith("/folders/")) {
                    // Handle folder deep link
                    val folderId = path.removePrefix("/folders/")
                    Log.d("NoteDetailActivity", "Deep link for folder ID: $folderId")

                    // Close this activity and open folder view
                    val folderIntent = Intent(this, NoteActivity::class.java).apply {
                        putExtra("folder_id", folderId)
                        putExtra("from_qr_code", true)
                    }
                    startActivity(folderIntent)
                    finish()
                }
            }
        }
    }
    private fun showCollaborateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_collaborator, null)
        val editTextEmail = dialogView.findViewById<android.widget.EditText>(R.id.et_email)

        AlertDialog.Builder(this)
            .setTitle("Add Collaborator")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val email = editTextEmail.text.toString().trim()
                if (email.isNotEmpty()) {
                    addCollaborator(email)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addCollaborator(email: String) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE

            try {
                val result = repository.shareNoteWithUser(noteId, email)

                if (result.isSuccess) {
                    Toast.makeText(this@NoteDetailActivity, "Collaborator added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@NoteDetailActivity, "Failed to add collaborator", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoteDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Lưu trang hiện tại với cơ chế đồng bộ
        runBlocking {
            try {
                val saveJob = lifecycleScope.launch {
                    saveCurrentPage(false)
                }

                // Đợi việc lưu hoàn tất với timeout
                withTimeout(3000) {
                    saveJob.join()
                }

                Log.d("NoteDetailActivity", "Page saved successfully in onPause")
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error saving page in onPause", e)
            }
        }

        // Cập nhật trạng thái typing người dùng
        collaborationManager.setUserTyping(false)
    }


    override fun onDestroy() {
        try {
            // Lưu trang hiện tại với cơ chế đồng bộ
            if (currentPage != null) {
                runBlocking {
                    try {
                        val saveJob = lifecycleScope.launch {
                            saveCurrentPage(false)
                        }

                        // Đợi việc lưu hoàn tất với timeout
                        withTimeout(3000) {
                            saveJob.join()
                        }

                        Log.d("NoteDetailActivity", "Page saved successfully in onDestroy")
                    } catch (e: Exception) {
                        Log.e("NoteDetailActivity", "Error saving in onDestroy", e)
                    }
                }
            }

            // Dọn dẹp
            binding.saveProgressBar.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            drawingManager?.cleanup()
            collaborationManager.cleanup()
        } catch (e: Exception) {
            Log.e("NoteDetailActivity", "Error during cleanup", e)
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        // Do not reload note data here - it's already loaded in onCreate

        // Just update UI elements if needed
        updateNavigationButtons()
        updatePageIndicator()
    }
}