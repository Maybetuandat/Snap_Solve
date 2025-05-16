package com.example.app_music.presentation.noteScene

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.app_music.R
import com.example.app_music.presentation.noteScene.model.NoteItem
import com.example.app_music.presentation.noteScene.noteAdapter.NotesAdapter
import com.example.app_music.presentation.utils.StorageManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NoteActivity"
        private const val REQUEST_IMAGE_PICK = 100
        private const val REQUEST_IMAGE_CAPTURE = 101
        private const val PERMISSION_REQUEST_CODE = 200
    }

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

    // Để lưu trữ thông tin ảnh
    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null

    // Storage manager
    private lateinit var storageManager: StorageManager

    // Danh sách quyền cần thiết
    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        // Khởi tạo StorageManager
        storageManager = StorageManager(this)

        // Khởi tạo UI elements
        titleTextView = findViewById(R.id.text_title)
        backButton = findViewById(R.id.button_back_note)
        addNewButton = findViewById(R.id.note_button_menu)

        // Thiết lập back button
        setupBackButton()

        // Thiết lập nút filter kiểu
        setupTypeButton()

        // Tải animation GIF
        loadGifAnimation()

        // Tải dữ liệu đã lưu
        loadSavedNotes()

        // Thiết lập RecyclerView
        setupRecyclerView()

        // Thiết lập nút menu
        setupMenuButton()

        // Kiểm tra và yêu cầu quyền cần thiết
        checkAndRequestPermissions()
    }

    private fun loadSavedNotes() {
        // Xóa danh sách cũ
        mainNotesList.clear()
        notesList.clear()

        // Tải dữ liệu từ StorageManager
        val savedNotes = storageManager.loadNotesList()

        // Thêm vào danh sách chính
        mainNotesList.addAll(savedNotes)
        notesList.addAll(savedNotes)

        Log.d(TAG, "Đã tải ${mainNotesList.size} note từ bộ nhớ")
    }

    override fun onPause() {
        super.onPause()
        // Lưu danh sách notes khi đóng ứng dụng hoặc chuyển màn hình
        saveNotes()
    }

    private fun saveNotes() {
        // Lưu danh sách note chính vào bộ nhớ
        storageManager.saveNotesList(mainNotesList)
        Log.d(TAG, "Đã lưu ${mainNotesList.size} note vào bộ nhớ")
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = ArrayList<String>()

        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (!allGranted) {
                Toast.makeText(
                    this,
                    "Ứng dụng cần các quyền này để hoạt động đầy đủ",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            if (isInFolder) {
                // Quay lại thư mục trước đó hoặc màn hình chính
                navigateBack()
            } else {
                // Hành vi back tiêu chuẩn khi ở màn hình chính
                onBackPressed()
            }
        }
    }

    private fun navigateBack() {
        if (folderHistory.isEmpty()) {
            // Nếu lịch sử trống, quay lại danh sách notes chính
            showMainNotesList()
        } else {
            // Lấy thư mục cuối cùng từ lịch sử
            val previousFolder = folderHistory.removeAt(folderHistory.size - 1)

            if (previousFolder.folderId == null) {
                // Nếu thư mục trước đó là màn hình chính
                showMainNotesList()
            } else {
                // Điều hướng đến thư mục trước đó
                previousFolder.folderId?.let { folderId ->
                    previousFolder.folderTitle?.let { folderTitle ->
                        openFolder(folderId, folderTitle, false)
                    }
                }
            }
        }
    }

    private fun showMainNotesList() {
        // Đặt lại trạng thái
        isInFolder = false
        currentFolderId = null
        currentFolderTitle = null
        folderHistory.clear()

        // Cập nhật UI
        titleTextView.text = getString(R.string.noteTitle)

        // Sử dụng menu gốc với tùy chọn tạo thư mục
        addNewButton.setOnClickListener {
            val popupMenu = PopupMenu(this, addNewButton)
            popupMenu.menuInflater.inflate(R.menu.menu_note_action, popupMenu.menu)
            enablePopupIcons(popupMenu)
            popupMenu.setOnMenuItemClickListener { item ->
                handleMenuItemClick(item.itemId)
            }
            popupMenu.show()
        }

        // Khôi phục danh sách notes gốc
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
                        btnType.text = getString(R.string.day)
                        sortNotesByDate()
                        true
                    }
                    R.id.type_name -> {
                        btnType.text = getString(R.string.name)
                        sortNotesByName()
                        true
                    }
                    R.id.type_type -> {
                        btnType.text = getString(R.string.type)
                        sortNotesByType()
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    private fun sortNotesByDate() {
        notesList.sortByDescending {
            try {
                val format = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
                format.parse(it.date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun sortNotesByName() {
        notesList.sortBy { it.title }
        adapter.notifyDataSetChanged()
    }

    private fun sortNotesByType() {
        // Sắp xếp theo kiểu: thư mục trước, sau đó đến note
        notesList.sortWith(compareBy({ !it.isFolder }, { it.title }))
        adapter.notifyDataSetChanged()
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
                // Mở nội dung thư mục
                openFolder(folder.id, folder.title, true)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun openFolder(folderId: String, folderTitle: String, addToHistory: Boolean) {
        // Lưu trạng thái hiện tại vào lịch sử nếu cần
        if (addToHistory) {
            folderHistory.add(FolderHistoryItem(currentFolderId, currentFolderTitle))
        }

        // Cập nhật thông tin thư mục hiện tại
        isInFolder = true
        currentFolderId = folderId
        currentFolderTitle = folderTitle

        // Cập nhật UI
        titleTextView.text = folderTitle

        // Thay đổi menu sang menu thư mục (không có tùy chọn tạo thư mục)
        addNewButton.setOnClickListener {
            val popupMenu = PopupMenu(this, addNewButton)
            popupMenu.menuInflater.inflate(R.menu.menu_folder_action, popupMenu.menu)
            enablePopupIcons(popupMenu)
            popupMenu.setOnMenuItemClickListener { item ->
                handleMenuItemClick(item.itemId)
            }
            popupMenu.show()
        }

        // Tải nội dung thư mục
        loadFolderContents(folderId)
    }

    private fun loadFolderContents(folderId: String) {
        // Xóa danh sách hiện tại
        notesList.clear()

        // Tìm các note thuộc thư mục này
        for (item in mainNotesList) {
            if (item.id == folderId && item.isFolder) {
                // Đây là thư mục cần tìm
                // Thêm tất cả các note con vào danh sách
                notesList.addAll(item.getChildNotes())
                break
            }
        }

        // Thông báo adapter
        adapter.notifyDataSetChanged()
    }

    private fun showNewItemOptions(anchorView: View?) {
        if (anchorView != null) {
            // Hiển thị menu ngay dưới view đã nhấp
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
            // Nếu không có view nào được truyền, sử dụng nút menu mặc định
            addNewButton.performClick()
        }
    }
    // Thêm vào NoteActivity.kt (trong onResume)
    override fun onResume() {
        super.onResume()

        // Cập nhật lại adapter để hiển thị thumbnail mới
        adapter.notifyDataSetChanged()

        // Hoặc tải lại toàn bộ dữ liệu nếu cần
        loadSavedNotes()
        setupRecyclerView()
    }
    private fun showItemOptions(anchorView: View, item: NoteItem) {
        // Hiển thị menu tùy chọn cho item (note hoặc thư mục)
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.menuInflater.inflate(R.menu.menu_item_options, popupMenu.menu)

        enablePopupIcons(popupMenu)

        // Đặt màu đỏ cho văn bản Xóa
        try {
            val menu = popupMenu.menu
            val deleteItem = menu.findItem(R.id.action_delete)
            deleteItem?.let {
                // Tạo SpannableString để đặt màu cho văn bản
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
                    // Hiển thị dialog đổi tên
                    showRenameDialog(item)
                    true
                }
                R.id.action_delete -> {
                    // Hiển thị xác nhận xóa
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
                // Mở file picker để chọn ảnh
                openFilePicker()
                true
            }
            R.id.action_createnote -> {
                // Sử dụng camera để chụp ảnh mới
                openCamera()
                true
            }
            R.id.action_createfolder -> {
                // Hiển thị dialog tạo thư mục mới
                showCreateFolderDialog()
                true
            }
            R.id.action_camera -> {
                // Sử dụng camera để chụp ảnh mới (giống action_createnote)
                openCamera()
                true
            }
            R.id.action_scan -> {
                // Tính năng quét sẽ được phát triển sau
                Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
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
        // Tạo dialog để đổi tên
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_name)
        editText.setText(item.title)

        AlertDialog.Builder(this)
            .setTitle(R.string.rename)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    // Cập nhật tên
                    renameItem(item, newName)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun renameItem(item: NoteItem, newName: String) {
        // Cập nhật tên item
        item.title = newName
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Đã đổi tên thành: $newName", Toast.LENGTH_SHORT).show()

        // Lưu thay đổi
        saveNotes()
    }

    private fun showDeleteConfirmation(item: NoteItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delete)
        builder.setMessage("Bạn có chắc chắn muốn xóa ${item.title}?")

        // Đặt màu đỏ cho nút Xóa
        builder.setPositiveButton(android.R.string.yes) { _, _ ->
            deleteItem(item)
        }
        builder.setNegativeButton(android.R.string.no, null)

        val dialog = builder.create()
        dialog.show()

        // Đặt màu đỏ cho nút tích cực (Xóa)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.delete_red))
    }

    private fun deleteItem(item: NoteItem) {
        // Nếu là note với ảnh, xóa ảnh từ bộ nhớ
        if (!item.isFolder) {
            storageManager.deleteNote(item.id)
        }

        // Xóa item từ danh sách hiển thị hiện tại
        notesList.remove(item)

        // Nếu đang trong thư mục, cần xóa khỏi cấu trúc dữ liệu chính
        if (isInFolder && currentFolderId != null) {
            // Tìm thư mục cha chứa item này
            for (mainItem in mainNotesList) {
                if (mainItem.id == currentFolderId && mainItem.isFolder) {
                    // Xóa item khỏi danh sách con của thư mục
                    val children = mainItem.getChildNotes().toMutableList()
                    children.remove(item)
                    break
                }
            }
        } else {
            // Nếu đang ở màn hình chính, xóa khỏi danh sách chính
            mainNotesList.remove(item)
        }

        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Đã xóa ${item.title}", Toast.LENGTH_SHORT).show()

        // Lưu thay đổi
        saveNotes()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), REQUEST_IMAGE_PICK)
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể mở trình chọn ảnh", Toast.LENGTH_SHORT).show()
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
                Log.e(TAG, "Lỗi khi tạo file ảnh: ${e.message}")
                Toast.makeText(this, "Không thể mở camera", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Không tìm thấy ứng dụng camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    try {
                        data?.data?.let { uri ->
                            // Lưu URI tạm thời và hiện dialog đặt tên
                            photoUri = uri
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                .format(Date())
                            showNoteNameDialog("Image_$timestamp")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi xử lý ảnh từ gallery: ${e.message}")
                        Toast.makeText(this, "Không thể xử lý ảnh đã chọn", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    try {
                        // Ảnh đã được lưu vào photoUri
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            .format(Date())
                        showNoteNameDialog("Camera_$timestamp")
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi xử lý ảnh từ camera: ${e.message}")
                        Toast.makeText(this, "Không thể xử lý ảnh vừa chụp", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showNoteNameDialog(defaultName: String = "") {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_name)

        if (defaultName.isNotEmpty()) {
            editText.setText(defaultName)
        }

        AlertDialog.Builder(this)
            .setTitle("Tên ghi chú")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val noteName = editText.text.toString().trim()
                if (noteName.isNotEmpty()) {
                    createNoteFromImage(noteName)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun createNoteFromImage(noteName: String) {
        photoUri?.let { uri ->
            // Hiển thị loading
            val progressDialog = ProgressDialog(this)
            progressDialog.setMessage("Đang xử lý ảnh...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            // Xử lý trên thread riêng
            Thread {
                val newNote = storageManager.createNoteFromUri(uri, noteName)

                runOnUiThread {
                    progressDialog.dismiss()

                    if (newNote != null) {
                        // Thêm note vào danh sách và cập nhật adapter
                        if (isInFolder && currentFolderId != null) {
                            // Thêm note vào CUỐI thư mục hiện tại
                            notesList.add(newNote) // Thêm vào cuối danh sách

                            // Tìm và cập nhật thư mục trong danh sách chính
                            for (mainItem in mainNotesList) {
                                if (mainItem.id == currentFolderId) {
                                    mainItem.addChildNote(newNote) // Thêm vào cuối thư mục
                                    break
                                }
                            }
                        } else {
                            // Thêm vào CUỐI danh sách chính
                            notesList.add(newNote) // Thêm vào cuối danh sách
                            mainNotesList.add(newNote) // Thêm vào cuối danh sách
                        }

                        adapter.notifyDataSetChanged()
                        Toast.makeText(this, "Đã tạo ghi chú mới", Toast.LENGTH_SHORT).show()

                        // Lưu thay đổi
                        saveNotes()
                    } else {
                        Toast.makeText(this, "Không thể tạo ghi chú mới", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }


    private fun showCreateFolderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_name)

        AlertDialog.Builder(this)
            .setTitle("Tạo thư mục mới")
            .setView(dialogView)
            .setPositiveButton("Tạo") { _, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    createNewFolder(folderName)
                } else {
                    Toast.makeText(this, "Vui lòng nhập tên thư mục", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun createNewFolder(folderName: String) {
        // Tạo ID duy nhất cho folder
        val folderId = "folder_${System.currentTimeMillis()}"

        // Định dạng ngày hiện tại
        val dateFormat = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        // Tạo folder mới (đánh dấu isFolder=true)
        val newFolder = NoteItem(folderId, folderName, currentDate, true)

        if (isInFolder && currentFolderId != null) {
            // Thêm folder mới vào CUỐI thư mục hiện tại
            notesList.add(newFolder) // Thêm vào cuối danh sách

            // Thêm folder mới vào thư mục cha trong danh sách chính
            for (mainItem in mainNotesList) {
                if (mainItem.id == currentFolderId) {
                    mainItem.addChildNote(newFolder) // Thêm vào cuối thư mục con
                    break
                }
            }
        } else {
            // Thêm folder mới vào CUỐI danh sách chính
            notesList.add(newFolder) // Thêm vào cuối danh sách
            mainNotesList.add(newFolder) // Thêm vào cuối danh sách
        }

        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Đã tạo thư mục mới", Toast.LENGTH_SHORT).show()

        // Lưu thay đổi
        saveNotes()
    }

    // Lớp helper để lưu trữ lịch sử thư mục
    data class FolderHistoryItem(val folderId: String?, val folderTitle: String?)

    // Xử lý nút back
    override fun onBackPressed() {
        if (isInFolder) {
            navigateBack()
        } else {
            super.onBackPressed()
        }
    }
}