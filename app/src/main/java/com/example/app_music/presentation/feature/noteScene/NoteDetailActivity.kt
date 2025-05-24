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

        // Set up UI
        setupToolbar()
        setupDrawingTools()
        setupPagination()

        if (intent.data != null) {
            handleDeepLink(intent)
        } else {
            noteId = intent.getStringExtra("note_id") ?: ""
            Log.d("NoteDetailActivity", noteId)
            noteTitle = intent.getStringExtra("note_title") ?: ""
            fromQrCode = intent.getBooleanExtra("from_qr_code", false)

            if (noteId.isEmpty()) {
                finish()
                return
            }
            loadNote()
        }

        // Initialize collaboration manager
        collaborationManager = CollaborationManager(noteId, this)

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

        startAutoSave()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = noteTitle
            setDisplayHomeAsUpEnabled(true) // hiển thị nút home bên trái
            setDisplayShowHomeEnabled(true) //hiển thị biểu tượng home
        }
    }

    private fun setupDrawingTools() {
        selectMode(DrawingView.DrawMode.PAN)

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

        //Draw Complete
        binding.drawingView.setOnDrawCompletedListener(object : DrawingView.OnDrawCompletedListener {
            override fun onDrawCompleted() {
                if (binding.drawingView.isForceSaveNeeded()) {
                    saveCurrentPage(false)
                }
            }
        })
    }

    private fun selectMode(mode: DrawingView.DrawMode) {
        binding.drawingView.setMode(mode)

        // Set đậm nhạt của button
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

        when (mode) {
            DrawingView.DrawMode.DRAW -> {
                binding.drawingView.setColor(currentColor)
                binding.drawingView.setStrokeWidth(currentWidth)
            }
            DrawingView.DrawMode.ERASE -> {
                binding.drawingView.setStrokeWidth(currentWidth * 2) // Tẩy rộng hơn 2 lần nét
            }
            else -> {
            }
        }
    }

    private fun setupPagination() {
        // Navigation buttons with more logging
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

        binding.buttonDeletePage?.setOnClickListener {
            showDeletePageConfirmation()
        }
        updateNavigationButtons()
        updatePageIndicator()
    }

    private fun updatePageIndicator() {
        if (pages.isEmpty()) {
            binding.textPageIndicator.text = "No pages"
        } else {
            binding.textPageIndicator.text = "Page ${currentPageIndex + 1} of ${pages.size}"
        }
    }

    private fun navigateToPreviousPage() {
        if (currentPageIndex > 0 && pages.isNotEmpty()) {
            val job = lifecycleScope.launch {
                try {
                    saveCurrentPage(false)
                    //đảm bảo lưu trang xong mới di chuyển trang
                    withContext(Dispatchers.Main) {
                        if (currentPageIndex > 0 && pages.isNotEmpty()) {
                            val newIndex = currentPageIndex - 1
                            loadPage(newIndex)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@NoteDetailActivity,
                        "Lỗi khi chuyển trang: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToNextPage() {
        if (currentPageIndex < pages.size - 1 && pages.isNotEmpty()) {
            val job = lifecycleScope.launch {
                try {
                    saveCurrentPage(false)
                    // Đảm bảo lưu xong mới chuyển trang
                    withContext(Dispatchers.Main) {
                        if (currentPageIndex < pages.size - 1 && pages.isNotEmpty()) {
                            val newIndex = currentPageIndex + 1
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

        // Enable/disable prev button
        binding.buttonPrevPage.isEnabled = canGoBack
        binding.buttonPrevPage.alpha = if (canGoBack) 1.0f else 0.5f

        // Enable/disable next button
        binding.buttonNextPage.isEnabled = canGoForward
        binding.buttonNextPage.alpha = if (canGoForward) 1.0f else 0.5f
    }

    private fun showAddPageDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_page, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

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

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null) {
            // Parse the URI
            val path = data.path
            if (path != null) {
                if (path.startsWith("/notes/")) {
                    val noteId = path.removePrefix("/notes/")
                    // Set the note ID
                    this.noteId = noteId
                    val pagesParam = data.getQueryParameter("pages")
                    if (pagesParam != null) {
                        val pageIds = pagesParam.split(",")
                        loadNoteWithSpecificPages(noteId, pageIds)
                    } else {
                        loadNote()
                    }
                } else if (path.startsWith("/folders/")) {
                    val folderId = path.removePrefix("/folders/")

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
                    pages.removeAt(deleteIndex)

                    for (i in deleteIndex until pages.size) {
                        val updatedPage = pages[i].copy(pageIndex = i)
                        pages[i] = updatedPage

                        // Lưu chỉ số mới vào Firebase
                        repository.updatePage(updatedPage)
                    }

                    //Cập nhật lại pageIds trong note
                    val noteResult = repository.getNote(pageToDelete.noteId)
                    if (noteResult.isSuccess) {
                        val note = noteResult.getOrNull()!!
                        note.pageIds.clear()
                        note.pageIds.addAll(pages.map { it.id })
                        repository.updateNote(note)
                    }

                    //Thông báo cho các thiết bị khác
                    collaborationManager.emitPageEvent(
                        CollaborationManager.PageEventType.PAGE_DELETED,
                        pageToDelete.noteId,
                        pageToDelete.id,
                        deleteIndex,
                        pages.map { it.id }
                    )

                    // Điều hướng đến trang thích hợp
                    if (pages.isEmpty()) {
                        // Nếu không còn trang nào, tạo trang trắng mới
                        val blankPageResult = repository.createBlankPage(pageToDelete.noteId)
                        if (blankPageResult.isSuccess) {
                            val page = blankPageResult.getOrNull()!!
                            pages.add(page)
                            currentPageIndex = 0
                        }
                    } else {
                        // Chuyển đến trang trước đó hoặc trang tiếp theo nếu đây là trang đầu tiên
                        currentPageIndex = if (deleteIndex > 0) deleteIndex - 1 else 0
                    }

                    // Tải trang mới
                    loadPage(currentPageIndex)

                    Toast.makeText(this@NoteDetailActivity, "Page deleted", Toast.LENGTH_SHORT).show()
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

    private fun debouncedSaveCurrentPage() {
        saveJob?.cancel()

        saveJob = lifecycleScope.launch {
            delay(AUTO_SAVE_DELAY)
            saveCurrentPage(false)
        }
    }
    private fun setupCollaboration() {
        val currentUserId = collaborationManager.getCurrentUserId()
        val username = UserPreference.getUserName(this)

        // Lấy ID người dùng hiện tại cho UserPresenceView
        binding.activeUsersView.setCurrentUserId(currentUserId)

        // Đặt user là người trong danh sách rồi
        collaborationManager.setUserPresence(username, userColor)

        // Theo dõi người dùng với thời gian hết hạn
        lifecycleScope.launch {
            // Map để lưu thời gian hoạt động cuối cùng của mỗi người dùng
            val lastActiveTimes = mutableMapOf<String, Long>()
            // Set để lưu các ID người dùng đã biết (KHÔNG bao gồm user hiện tại)
            val knownUserIds = HashSet<String>()
            // Không thêm currentUserId vào knownUserIds để luôn hiển thị thông báo

            // Thời gian tính là không hoạt động (120 giây)
            val INACTIVE_THRESHOLD = 120000L

            collaborationManager.getActiveUsers().collectLatest { allUsers ->
                // Thời gian hiện tại
                val currentTime = System.currentTimeMillis()

                // Lọc ra người dùng đang hoạt động (không offline và trong thời gian hoạt động)
                val activeUsers = allUsers.filter { user ->
                    !user.isOffline && (currentTime - user.lastActive < INACTIVE_THRESHOLD)
                }

                // Lọc ra người dùng mới thực sự (KHÔNG loại trừ currentUserId)
                val newUsers = activeUsers.filter {
                    !knownUserIds.contains(it.userId) && it.userId != currentUserId
                }

                // Cập nhật UI - Hiển thị TẤT CẢ người dùng đang hoạt động
                binding.activeUsersView.updateActiveUsers(activeUsers)

                // Hiển thị tổng số người dùng (bao gồm cả bản thân)
                binding.userCount.text = "${activeUsers.size}"

                // Hiển thị thông báo cho người dùng mới (không bao gồm bản thân)
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
                val noteResult = repository.getNote(noteId)

                if (noteResult.isSuccess) {
                    val note = noteResult.getOrNull()!!

                    if (noteTitle.isEmpty()) {
                        noteTitle = note.title
                        supportActionBar?.title = noteTitle
                    }

                    // Load pages
                    if (note.pageIds.isEmpty()) {
                        val blankPageResult = repository.createBlankPage(noteId)
                        if (blankPageResult.isSuccess) {
                            val page = blankPageResult.getOrNull()!!
                            pages.add(page)
                        } else {
                            Toast.makeText(this@NoteDetailActivity,
                                "Failed to create first page", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val pagesResult = repository.getPages(noteId)
                        if (pagesResult.isSuccess) {
                            pages.clear()
                            pages.addAll(pagesResult.getOrNull()!!)
                            pages.sortBy { it.pageIndex }
                        } else {
                            Log.e("NoteDetailActivity", "Failed to load pages: ${pagesResult.exceptionOrNull()}")
                        }
                    }

                    // Load trang dau tien
                    if (pages.isNotEmpty()) {
                        currentPageIndex = 0
                        loadPage(currentPageIndex)
                    }

                    // Update nut chueyenr trang
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

                    // Ưu tiên tải dữ liệu vector nếu có, nếu không thì dùng lại cái cũ
                    if (!note.vectorDrawingData.isNullOrEmpty()) {
                        try {
                            binding.drawingView.setDrawingDataFromJson(note.vectorDrawingData!!)
                        } catch (e: Exception) {
                            note.drawingData?.let { loadBitmapDrawingData(it) }
                        }
                    } else if (note.drawingData != null) {
                        loadBitmapDrawingData(note.drawingData!!)
                    }
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error in loadNote", e)
            } finally {
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
            } else {
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

        // Xóa dữ liệu cũ đi
        clearPageContent()

        // Cập nhật trang hiện tại
        currentPage = pages[index]
        currentPageIndex = index

        // Tải trang mới với timeout
        lifecycleScope.launch {
            try {
                val page = pages[index]

                if (currentPage?.id != targetPageId) {
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
                                if (currentPage?.id == targetPageId) {
                                    withContext(Dispatchers.Main) {
                                        binding.drawingView.setBackgroundImage(bitmap)
                                        backgroundLoaded = true
                                    }
                                } else {
                                    return@withTimeout
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("NoteDetailActivity", "Error loading background image: ${e.message}")
                    }
                }

                if (currentPage?.id != targetPageId) {
                    return@launch
                }

                // Tạo nền trắng nếu không tải được ảnh
                if (!backgroundLoaded) {
                    withContext(Dispatchers.Main) {
                        binding.drawingView.setWhiteBackground(800, 1200)
                    }
                }

                // Delay để đảm bảo UI được cập nhật
                delay(100)

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
                        page.id,
                        this@NoteDetailActivity
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

        // Xóa background trong DrawingView
        binding.drawingView.setBackgroundImage(null)

        // Xóa tham chiếu đến ảnh trong ImageView
        binding.imageNote.setImageDrawable(null)
        binding.imageNote.visibility = View.GONE

        // phóng bộ nhớ
        System.gc()
    }

    private fun setupPageEventListener() {
        lifecycleScope.launch {
            collaborationManager.getPageEventsFlow().collectLatest { event ->
                when (event.getEventType()) {
                    CollaborationManager.PageEventType.PAGE_DELETED -> {
                        handleRemotePageDeletion(event.pageId, event.allPageIds)
                    }
                    CollaborationManager.PageEventType.PAGE_ADDED -> {
                        handleRemotePageAddition(event.pageId, event.pageIndex)
                    }
                    else->{}
                }
            }
        }
    }

    private fun handleRemotePageAddition(pageId: String, pageIndex: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val pageResult = repository.getPage(pageId)

                if (pageResult.isSuccess) {
                    val newPage = pageResult.getOrNull()!!

                    // Kiểm tra xem có page đó chưa
                    if (pages.none { it.id == pageId }) {
                        // Thêm page vào đúng index
                        if (pageIndex >= 0 && pageIndex <= pages.size) {
                            pages.add(pageIndex, newPage)

                            // Cập nhật số lượng
                            for (i in 0 until pages.size) {
                                if (pages[i].pageIndex != i) {
                                    pages[i] = pages[i].copy(pageIndex = i)
                                }
                            }

                            // Cập nhật số lượng page
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
                // Xóa page ở list local
                val deletedIndex = pages.indexOfFirst { it.id == deletedPageId }
                if (deletedIndex >= 0) {
                    pages.removeAt(deletedIndex)

                    // Cập nhật so trang
                    if (currentPageIndex >= pages.size) {
                        currentPageIndex = maxOf(0, pages.size - 1)
                    } else if (currentPageIndex > deletedIndex) {
                        //Cap nhat so luong
                        currentPageIndex--
                    }

                    // neu xoa trang hien tai thi update trang moi
                    if (currentPage?.id == deletedPageId) {
                        if (pages.isNotEmpty()) {
                            loadPage(currentPageIndex)
                        } else {
                            binding.drawingView.clearDrawing()
                            binding.imageNote.setImageDrawable(null)
                        }
                    }

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

                    val result = repository.updatePage(updatedPage)

                    if (result.isSuccess) {
                        currentPage = result.getOrNull()
                        pages[currentPageIndex] = currentPage!!

                    } else {
                        Log.e("NoteDetailActivity", "Error saving page: ${result.exceptionOrNull()}")
                    }
                }
            } catch (e: CancellationException) {
                Log.d("NoteDetailActivity", "Save operation was cancelled during activity shutdown")
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error in saveCurrentPage", e)
            } finally {
                binding.saveProgressBar.visibility = View.GONE
            }
        }
    }

    private var autoSaveJob: Job? = null
    private val AUTO_SAVE_INTERVAL = 10000L // 10 giây

    private fun startAutoSave() {
        autoSaveJob?.cancel()

        // Khởi tạo job mới với try-catch để xử lý hủy job
        autoSaveJob = lifecycleScope.launch(Dispatchers.Default + SupervisorJob()) {
            try {
                while (isActive) {
                    delay(AUTO_SAVE_INTERVAL)

                    // Kiểm tra nếu có trang hiện tại
                    val currentPageCopy = currentPage
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

    private fun addBlankPage() {
        binding.progressBar.visibility = View.VISIBLE

        Toast.makeText(this, "Creating blank page...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // Create a new blank page
                val pageResult = repository.createBlankPage(noteId)

                if (pageResult.isSuccess) {
                    val newPage = pageResult.getOrNull()!!

                    pages.add(newPage)
                    collaborationManager.emitPageEvent(
                        CollaborationManager.PageEventType.PAGE_ADDED,
                        noteId,
                        newPage.id,
                        pages.size - 1
                    )

                    updatePageIndicator()
                    updateNavigationButtons()

                    // chuyen den trang moi
                    val newIndex = pages.size - 1
                    loadPage(newIndex)
                    binding.drawingView.resetTransform()
                    Toast.makeText(this@NoteDetailActivity,
                        "Blank page added", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("NoteDetailActivity", "Failed to add blank page: ${pageResult.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error adding blank page", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                CAMERA_PERMISSION_CODE
            )
            return
        }

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"

            val storageDir = cacheDir
            val photoFile = File.createTempFile(imageFileName, ".jpg", storageDir)

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

        Toast.makeText(this, "Creating image page...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        if (tempPhotoPath != null && tempPhotoUri != null &&
                            imageUri.toString() == tempPhotoUri.toString()) {
                            BitmapFactory.decodeFile(tempPhotoPath)
                        } else {
                            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                        }
                    } catch (e: Exception) {
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


    override fun onPause() {
        super.onPause()

        lifecycleScope.launch {
            try {
                withContext(NonCancellable) {
                    saveCurrentPage(false)
                }
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error in onPause", e)
            }
        }

        collaborationManager.setUserTyping(false)
    }


    override fun onDestroy() {
        try {

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