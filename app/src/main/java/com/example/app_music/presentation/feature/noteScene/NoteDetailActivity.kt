package com.example.app_music.presentation.feature.noteScene

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.UUID
import android.Manifest
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.presentation.feature.noteScene.views.RealtimeDrawingManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

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
        binding.buttonColor.setOnClickListener {
            showColorPicker()
        }

        binding.buttonStrokeWidth.setOnClickListener {
            showStrokeWidthDialog()
        }

        // Set draw completed listener with forced save check
        binding.drawingView.setOnDrawCompletedListener(object : DrawingView.OnDrawCompletedListener {
            override fun onDrawCompleted() {

                // Check if force save is needed
                if (binding.drawingView.isForceSaveNeeded()) {
                    saveCurrentPage(false)
                }
            }
        })
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

                    // Notify all collaborators of the page deletion
                    collaborationManager.emitPageEvent(
                        CollaborationManager.PageEventType.PAGE_DELETED,
                        pageToDelete.noteId,
                        pageToDelete.id,
                        deleteIndex,
                        pages.map { it.id }
                    )

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
                            collaborationManager.emitPageEvent(
                                CollaborationManager.PageEventType.PAGE_ADDED,
                                pageToDelete.noteId,
                                page.id,
                                0,
                                pages.map { it.id }
                            )
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
        // Navigation buttons with more logging
        binding.buttonPrevPage.setOnClickListener {
            Log.d("NoteDetailActivity", "Previous page button clicked")
            navigateToPreviousPage()
        }

        binding.buttonNextPage.setOnClickListener {
            Log.d("NoteDetailActivity", "Next page button clicked")
            navigateToNextPage()
        }

        binding.buttonAddPage.setOnClickListener {
            Log.d("NoteDetailActivity", "Add page button clicked")
            showAddPageDialog()
        }

        binding.fabAddPage.setOnClickListener {
            Log.d("NoteDetailActivity", "Floating add page button clicked")
            showAddPageDialog()
        }

        // Thêm xử lý nút xóa trang nếu có
        binding.buttonDeletePage?.setOnClickListener {
            Log.d("NoteDetailActivity", "Delete page button clicked")
            showDeletePageConfirmation()
        }

        // Cập nhật trạng thái nút
        updateNavigationButtons()
        updatePageIndicator()
    }

    private fun setupHelpButtons() {
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
    private fun setupCollaboration() {
        // Lấy ID người dùng hiện tại
        val currentUserId = currentUser?.uid ?: collaborationManager.getCurrentUserId()
        val username = currentUser?.displayName ?: UserPreference.getUserName(this)

        // Đặt ID người dùng hiện tại cho UserPresenceView
        binding.activeUsersView.setCurrentUserId(currentUserId)

        // Đánh dấu người dùng hiện tại là có mặt
        collaborationManager.setUserPresence(username, userColor)

        // Theo dõi người dùng với thời gian hết hạn
        lifecycleScope.launch {
            // Map để lưu thời gian hoạt động cuối cùng của mỗi người dùng
            val lastActiveTimes = mutableMapOf<String, Long>()
            // Set để lưu các ID người dùng đã biết
            val knownUserIds = HashSet<String>()
            knownUserIds.add(currentUserId) // Thêm ID của chính mình

            // Thời gian tính là không hoạt động (10 giây)
            val INACTIVE_THRESHOLD = 10000L

            collaborationManager.getActiveUsers().collectLatest { allUsers ->
                // Thời gian hiện tại
                val currentTime = System.currentTimeMillis()

                // Lọc ra người dùng đang hoạt động (không offline và trong thời gian hoạt động)
                val activeUsers = allUsers.filter { user ->
                    !user.isOffline && (currentTime - user.lastActive < INACTIVE_THRESHOLD)
                }

                // Lọc ra người dùng mới thực sự
                val newUsers = activeUsers.filter {
                    !knownUserIds.contains(it.userId) && it.userId != currentUserId
                }

                // Cập nhật UI
                binding.activeUsersView.updateActiveUsers(activeUsers)

                // Chỉ hiển thị số lượng người dùng khác
                val otherUsers = activeUsers.filter { it.userId != currentUserId }
                binding.userCount.text = "${otherUsers.size}"

                // Hiển thị thông báo cho người dùng mới
                for (newUser in newUsers) {
                    Toast.makeText(
                        this@NoteDetailActivity,
                        "${newUser.username} đã tham gia",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Thêm vào danh sách đã biết
                    knownUserIds.add(newUser.userId)
                }

                // Cập nhật thời gian hoạt động cuối
                for (user in activeUsers) {
                    lastActiveTimes[user.userId] = user.lastActive
                }
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
            clearPageContent()
            updatePageIndicator()
            updateNavigationButtons()
            return
        }

        if (index < 0 || index >= pages.size) {
            Log.e("NoteDetailActivity", "Invalid page index: $index")
            return
        }

        // Hiển thị loading indicator
        binding.progressBar.visibility = View.VISIBLE

        // Lưu biến tới page ID cho việc kiểm tra sau
        val targetPageId = pages[index].id

        // XÓA DỮ LIỆU CŨ
        clearPageContent()

        // Cập nhật trang hiện tại
        currentPage = pages[index]
        currentPageIndex = index

        // Tải trang mới với timeout
        lifecycleScope.launch {
            try {
                val page = pages[index]

                // Đảm bảo ta vẫn đang tải đúng trang
                if (currentPage?.id != targetPageId) {
                    Log.d("NoteDetailActivity", "Page changed during loading, aborting load of $targetPageId")
                    return@launch
                }

                // Tải ảnh nền
                var backgroundLoaded = false
                if (page.imagePath != null) {
                    try {
                        // Tải ảnh với timeout
                        withTimeout(5000) {
                            val bitmap = storageManager.loadPageImage(page.id)

                            if (bitmap != null) {
                                // Kiểm tra lại xem chúng ta vẫn đang tải đúng trang
                                if (currentPage?.id == targetPageId) {
                                    withContext(Dispatchers.Main) {
                                        binding.drawingView.setBackgroundImage(bitmap)
                                        Log.d("NoteDetailActivity", "Background image loaded successfully for page ${page.id}")
                                        backgroundLoaded = true
                                    }
                                } else {
                                    Log.d("NoteDetailActivity", "Page changed during bitmap loading, aborting")
                                    return@withTimeout
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("NoteDetailActivity", "Error loading background image: ${e.message}")
                    }
                }

                // Đảm bảo ta vẫn đang tải đúng trang
                if (currentPage?.id != targetPageId) {
                    Log.d("NoteDetailActivity", "Page changed after image loading, aborting load")
                    return@launch
                }

                // Tạo nền trắng nếu không tải được ảnh
                if (!backgroundLoaded) {
                    withContext(Dispatchers.Main) {
                        binding.drawingView.setWhiteBackground(800, 1200)
                        Log.d("NoteDetailActivity", "Created white background")
                    }
                }

                // Delay để đảm bảo UI được cập nhật
                delay(100)

                // Đảm bảo ta vẫn đang tải đúng trang
                if (currentPage?.id != targetPageId) {
                    Log.d("NoteDetailActivity", "Page changed before drawing manager creation, aborting")
                    return@launch
                }

                // Khởi tạo drawing manager
                withContext(Dispatchers.Main) {
                    drawingManager = RealtimeDrawingManager(
                        binding.drawingView,
                        collaborationManager,
                        lifecycleScope,
                        page.id
                    )

                    // Cập nhật UI
                    updatePageIndicator()
                    updateNavigationButtons()

                    Log.d("NoteDetailActivity", "Page ${page.id} loaded successfully")
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error loading page", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@NoteDetailActivity,
                        "Lỗi tải trang: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun clearPageContent() {
        Log.d("NoteDetailActivity", "Đang xóa nội dung trang")

        // Dọn dẹp DrawingManager hiện tại
        drawingManager?.cleanup()
        drawingManager = null

        // Reset DrawingView về trạng thái ban đầu
        binding.drawingView.resetTransform()
        binding.drawingView.clearDrawing(false)

        // Xóa RÕRÀNG background trong DrawingView
        binding.drawingView.setBackgroundImage(null)

        // Xóa tham chiếu đến ảnh trong ImageView
        binding.imageNote.setImageDrawable(null)
        binding.imageNote.visibility = View.GONE

        // Buộc giải phóng bộ nhớ
        System.gc()
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

                // Sử dụng NonCancellable để đảm bảo công việc hoàn thành ngay cả khi job cha bị hủy
                withContext(NonCancellable) {
                    // Cập nhật timestamp cho trang
                    val updatedPage = currentPage!!.copy(
                        createdAt = System.currentTimeMillis()
                    )

                    // Lưu trang vào repository
                    val result = repository.updatePage(updatedPage)

                    if (result.isSuccess) {
                        // Cập nhật bản sao cục bộ
                        currentPage = result.getOrNull()
                        pages[currentPageIndex] = currentPage!!

                        Log.d("NoteDetailActivity", "Page saved successfully")
                    } else {
                        Log.e("NoteDetailActivity", "Error saving page: ${result.exceptionOrNull()}")
                    }
                }

                // Hiển thị toast nếu cần (bên ngoài NonCancellable)
                if (showToast) {
                    Toast.makeText(this@NoteDetailActivity, "Page saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: CancellationException) {
                // Đánh log là bình thường, không phải lỗi
                Log.d("NoteDetailActivity", "Save operation was cancelled during activity shutdown")
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error in saveCurrentPage", e)

                if (showToast) {
                    Toast.makeText(this@NoteDetailActivity,
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                binding.saveProgressBar.visibility = View.GONE
            }
        }
    }

    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        // Scale the bitmap to thumbnail size (e.g., 200x200)
        val width = bitmap.width
        val height = bitmap.height
        val maxSize = 200

        val scale = Math.min(
            maxSize.toFloat() / width.toFloat(),
            maxSize.toFloat() / height.toFloat()
        )

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    // Add an automatic save timer
    private var autoSaveJob: Job? = null
    private val AUTO_SAVE_INTERVAL = 10000L // 10 giây

    private fun startAutoSave() {
        // Hủy job cũ nếu có
        autoSaveJob?.cancel()

        // Khởi tạo job mới với try-catch để xử lý hủy job
        autoSaveJob = lifecycleScope.launch(Dispatchers.Default + SupervisorJob()) {
            try {
                while (isActive) {
                    delay(AUTO_SAVE_INTERVAL)

                    // Kiểm tra nếu có trang hiện tại
                    val currentPageCopy = currentPage // Lấy bản sao để tránh lỗi null
                    if (currentPageCopy != null) {
                        // Kiểm tra xem trang có bị chỉnh sửa không
                        val lastDrawingTime = binding.drawingView.getLastEditTime() ?: 0L
                        val lastSaveTime = lastEditTimes[currentPageCopy.id] ?: 0L

                        if (lastDrawingTime > lastSaveTime) {
                            try {
                                // Thực hiện lưu trên luồng chính
                                withContext(Dispatchers.Main) {
                                    saveCurrentPage(false)
                                }
                                // Cập nhật thời gian lưu
                                lastEditTimes[currentPageCopy.id] = System.currentTimeMillis()
                                Log.d("NoteDetailActivity", "Auto-saved page ${currentPageCopy.id}")
                            } catch (e: CancellationException) {
                                // Bỏ qua lỗi hủy job
                                Log.d("NoteDetailActivity", "Auto-save job canceled normally")
                            } catch (e: Exception) {
                                Log.e("NoteDetailActivity", "Error in auto-save", e)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Bỏ qua lỗi hủy job
                Log.d("NoteDetailActivity", "Auto-save job canceled normally")
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error in auto-save job", e)
            }
        }
    }

    private fun navigateToPreviousPage() {
        if (currentPageIndex > 0 && pages.isNotEmpty()) {
            // Lưu trang hiện tại trước khi chuyển
            val job = lifecycleScope.launch {
                try {
                    saveCurrentPage(false)

                    // Đảm bảo lưu xong mới chuyển trang
                    withContext(Dispatchers.Main) {
                        // Kiểm tra lại điều kiện để tránh đua dữ liệu
                        if (currentPageIndex > 0 && pages.isNotEmpty()) {
                            val newIndex = currentPageIndex - 1
                            Log.d("NoteDetailActivity", "Navigating to previous page: $newIndex")
                            loadPage(newIndex)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NoteDetailActivity", "Error navigating to previous page", e)
                    Toast.makeText(this@NoteDetailActivity,
                        "Lỗi khi chuyển trang: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToNextPage() {
        if (currentPageIndex < pages.size - 1 && pages.isNotEmpty()) {
            // Lưu trang hiện tại trước khi chuyển
            val job = lifecycleScope.launch {
                try {
                    saveCurrentPage(false)

                    // Đảm bảo lưu xong mới chuyển trang
                    withContext(Dispatchers.Main) {
                        // Kiểm tra lại điều kiện để tránh đua dữ liệu
                        if (currentPageIndex < pages.size - 1 && pages.isNotEmpty()) {
                            val newIndex = currentPageIndex + 1
                            Log.d("NoteDetailActivity", "Navigating to next page: $newIndex")
                            loadPage(newIndex)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NoteDetailActivity", "Error navigating to next page", e)
                    Toast.makeText(this@NoteDetailActivity,
                        "Lỗi khi chuyển trang: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun updateNavigationButtons() {
        val canGoBack = currentPageIndex > 0
        val canGoForward = currentPageIndex < pages.size - 1

        Log.d("NoteDetailActivity", "updateNavigationButtons: canGoBack=$canGoBack, canGoForward=$canGoForward, " +
                "currentIndex=$currentPageIndex, totalPages=${pages.size}")

        // Enable/disable prev button
        binding.buttonPrevPage.isEnabled = canGoBack
        binding.buttonPrevPage.alpha = if (canGoBack) 1.0f else 0.5f

        // Enable/disable next button
        binding.buttonNextPage.isEnabled = canGoForward
        binding.buttonNextPage.alpha = if (canGoForward) 1.0f else 0.5f
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
                    collaborationManager.emitPageEvent(
                        CollaborationManager.PageEventType.PAGE_ADDED,
                        noteId,
                        newPage.id,
                        pages.size - 1
                    )

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
                    collaborationManager.emitPageEvent(
                        CollaborationManager.PageEventType.PAGE_ADDED,
                        noteId,
                        page.id,
                        pages.size - 1
                    )
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

    override fun onPause() {
        super.onPause()

        // Lưu trang hiện tại với cơ chế đồng bộ
        lifecycleScope.launch {
            try {
                // Lưu trang hiện tại một cách an toàn
                withContext(NonCancellable) {
                    saveCurrentPage(false)
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error in onPause", e)
            }
        }

        // Cập nhật trạng thái typing người dùng
        collaborationManager.setUserTyping(false)
    }


    override fun onDestroy() {
        try {
            // Hủy job auto save
            autoSaveJob?.cancel()

            // Tạo một coroutine scope mới độc lập với lifecycle
            val cleanupJob = GlobalScope.launch(Dispatchers.IO + NonCancellable) {
                try {
                    if (currentPage != null) {
                        // Lưu trang cuối cùng
                        saveCurrentPage(false)
                        Log.d("NoteDetailActivity", "Final save successful")
                    }
                } catch (e: Exception) {
                    Log.e("NoteDetailActivity", "Error during final save", e)
                } finally {
                    // Đảm bảo dọn dẹp các tài nguyên
                    drawingManager?.cleanup()
                    collaborationManager.removeUserPresence()
                    collaborationManager.cleanup()
                }
            }

            // Đợi tối đa 2 giây cho việc lưu hoàn tất
            runBlocking(Dispatchers.IO) {
                try {
                    withTimeout(2000) {
                        cleanupJob.join()
                    }
                } catch (e: Exception) {
                    Log.w("NoteDetailActivity", "Cleanup timed out, continuing with activity destruction")
                }
            }
        } catch (e: Exception) {
            Log.e("NoteDetailActivity", "Error during cleanup", e)
        }

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (noteTitle.isNotEmpty()) {
            supportActionBar?.title = noteTitle
        }
        // Just update UI elements if needed
        updateNavigationButtons()
        updatePageIndicator()
    }
}