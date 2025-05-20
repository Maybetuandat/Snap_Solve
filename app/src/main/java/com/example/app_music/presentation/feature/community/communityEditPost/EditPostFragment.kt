package com.example.app_music.presentation.feature.community.communityEditPost

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_music.R
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.domain.model.Topic
import com.example.app_music.presentation.feature.community.communityPosting.SelectedImagesAdapter
import com.example.app_music.presentation.feature.community.communityPosting.TopicSelectionBottomSheet
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.snackbar.Snackbar
import java.io.File

class EditPostFragment : Fragment() {
    private lateinit var viewModel: EditPostViewModel

    private lateinit var btnClose: ImageButton
    private lateinit var btnUpdate: TextView
    private lateinit var topicSelector: View
    private lateinit var tvTopicLabel: TextView
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var tvImageCount: TextView
    private lateinit var btnAddImage: ImageButton
    private lateinit var rvSelectedImages: RecyclerView
    private lateinit var tvSelectedImagesTitle: TextView
    private lateinit var tvSelectedTopicsTitle: TextView
    private lateinit var selectedTopicsContainer: FlexboxLayout
    private lateinit var progressBar: ProgressBar

    private val imageAdapter = SelectedImagesAdapter()
    private val selectedTopics = mutableListOf<Topic>()
    private val maxImageCount = 10

    private var postId: Long = -1L

    // Launcher để chọn nhiều ảnh
    private val getMultipleContent = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            handleSelectedImages(uris)
        }
    }

    // Launcher để yêu cầu quyền truy cập
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Snackbar.make(requireView(), "Cần quyền truy cập hình ảnh để tải lên", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy postId từ arguments
        postId = arguments?.getLong("postId") ?: -1L
        if (postId == -1L) {
            Snackbar.make(requireView(), "ID bài viết không hợp lệ", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[EditPostViewModel::class.java]

        // Tìm các view
        findViews(view)

        // Thiết lập RecyclerView cho ảnh đã chọn
        setupImagesRecyclerView()

        // Thiết lập các sự kiện
        setupListeners()

        // Quan sát ViewModel
        observeViewModel()

        // Tải dữ liệu bài viết và danh sách topic
        viewModel.loadPostForEdit(postId)
        viewModel.loadTopics()
    }

    private fun findViews(view: View) {
        btnClose = view.findViewById(R.id.btnClose)
        btnUpdate = view.findViewById(R.id.btnUpdate)
        topicSelector = view.findViewById(R.id.topicSelector)
        tvTopicLabel = view.findViewById(R.id.tvTopicLabel)
        etTitle = view.findViewById(R.id.etTitle)
        etContent = view.findViewById(R.id.etContent)
        tvImageCount = view.findViewById(R.id.tvImageCount)
        btnAddImage = view.findViewById(R.id.btnAddImage)
        rvSelectedImages = view.findViewById(R.id.rvSelectedImages)
        tvSelectedImagesTitle = view.findViewById(R.id.tvSelectedImagesTitle)
        tvSelectedTopicsTitle = view.findViewById(R.id.tvSelectedTopicsTitle)
        selectedTopicsContainer = view.findViewById(R.id.selectedTopicsContainer)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupImagesRecyclerView() {
        rvSelectedImages.layoutManager = GridLayoutManager(requireContext(), 3)
        rvSelectedImages.adapter = imageAdapter

        // Xử lý sự kiện xóa ảnh
        imageAdapter.setOnImageRemoveListener { position ->
            viewModel.removeImage(position)
        }
    }

    private fun setupListeners() {
        // Nút đóng - quay lại
        btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        // Nút cập nhật bài - kiểm tra và cập nhật bài
        btnUpdate.setOnClickListener {
            if (validatePost()) {
                updatePost()
            }
        }

        // Chọn topic - hiển thị dialog chọn topic
        topicSelector.setOnClickListener {
            showTopicSelector()
        }

        // Thêm ảnh - kiểm tra quyền và mở thư viện ảnh
        btnAddImage.setOnClickListener {
            checkPermissionAndOpenGallery()
        }
    }

    private fun observeViewModel() {
        // Quan sát bài viết được tải để chỉnh sửa
        viewModel.postToEdit.observe(viewLifecycleOwner) { post ->
            post?.let {
                populatePostData(it)
            }
        }

        // Quan sát danh sách topic
        viewModel.topics.observe(viewLifecycleOwner) { topics ->
            // Danh sách topic đã được tải
        }

        // Quan sát những ảnh đã chọn
        viewModel.selectedImages.observe(viewLifecycleOwner) { images ->
            imageAdapter.submitList(images)
            updateImageCounter()
        }

        // Quan sát trạng thái cập nhật bài
        viewModel.isUpdating.observe(viewLifecycleOwner) { isUpdating ->
            progressBar.visibility = if (isUpdating) View.VISIBLE else View.GONE
            btnUpdate.isEnabled = !isUpdating
            btnAddImage.isEnabled = !isUpdating
            topicSelector.isEnabled = !isUpdating
        }

        // Quan sát kết quả cập nhật bài
        viewModel.updateResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Cập nhật bài thành công", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        // Quan sát thông báo lỗi
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun populatePostData(post: com.example.app_music.domain.model.Post) {
        // Điền thông tin bài viết vào form
        etTitle.setText(post.title)
        etContent.setText(post.content)

        // Thiết lập topics đã chọn
        selectedTopics.clear()
        selectedTopics.addAll(post.topics)
        updateSelectedTopicsUI()

        // Thiết lập ảnh đã có (chuyển đổi từ URL thành URI)
        val existingImages = post.getAllImages()
        viewModel.setExistingImages(existingImages)
    }

    private fun checkPermissionAndOpenGallery() {
        if (viewModel.selectedImages.value?.size ?: 0 >= maxImageCount) {
            Toast.makeText(requireContext(), "Chỉ được chọn tối đa $maxImageCount ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Snackbar.make(requireView(), "Cần quyền truy cập hình ảnh để đăng bài với ảnh", Snackbar.LENGTH_LONG)
                    .setAction("Cấp quyền") {
                        requestPermissionLauncher.launch(permission)
                    }
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallery() {
        val remainingSlots = maxImageCount - (viewModel.selectedImages.value?.size ?: 0)

        if (remainingSlots <= 0) {
            Toast.makeText(requireContext(), "Đã đạt giới hạn số lượng ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        getMultipleContent.launch("image/*")
    }

    private fun handleSelectedImages(uris: List<Uri>) {
        val currentCount = viewModel.selectedImages.value?.size ?: 0
        val newCount = currentCount + uris.size

        if (newCount > maxImageCount) {
            val canAdd = maxImageCount - currentCount
            val message = "Chỉ có thể thêm $canAdd ảnh nữa. Đã đạt giới hạn $maxImageCount ảnh."
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

            val limitedUris = uris.take(canAdd)
            viewModel.addImages(limitedUris)
        } else {
            viewModel.addImages(uris)
        }
    }

    private fun updateImageCounter() {
        val count = viewModel.selectedImages.value?.size ?: 0
        tvImageCount.text = "$count/$maxImageCount"

        if (count >= maxImageCount) {
            btnAddImage.alpha = 0.5f
            tvImageCount.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
        } else {
            btnAddImage.alpha = 1.0f
            tvImageCount.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        if (count > 0) {
            rvSelectedImages.visibility = View.VISIBLE
            tvSelectedImagesTitle.visibility = View.VISIBLE
        } else {
            rvSelectedImages.visibility = View.GONE
            tvSelectedImagesTitle.visibility = View.GONE
        }
    }

    private fun showTopicSelector() {
        val topicBottomSheet = TopicSelectionBottomSheet.newInstance()

        // Thiết lập danh sách topic và những topic đã được chọn
        topicBottomSheet.setTopics(
            topics = viewModel.topics.value ?: emptyList(),
            preSelectedTopicIds = selectedTopics.map { it.id }.toSet()
        )

        // Thiết lập listener để nhận kết quả
        topicBottomSheet.setTopicSelectionListener(object : TopicSelectionBottomSheet.TopicSelectionListener {
            override fun onTopicsSelected(selectedTopics: List<Topic>) {
                this@EditPostFragment.selectedTopics.clear()
                this@EditPostFragment.selectedTopics.addAll(selectedTopics)
                updateSelectedTopicsUI()
            }
        })

        topicBottomSheet.show(parentFragmentManager, TopicSelectionBottomSheet.TAG)
    }

    private fun updateSelectedTopicsUI() {
        selectedTopicsContainer.removeAllViews()

        if (selectedTopics.isEmpty()) {
            tvSelectedTopicsTitle.visibility = View.GONE
            selectedTopicsContainer.visibility = View.GONE
            tvTopicLabel.text = getString(R.string.posting_choose_topic)
        } else {
            tvSelectedTopicsTitle.visibility = View.VISIBLE
            selectedTopicsContainer.visibility = View.VISIBLE

            if (selectedTopics.size == 1) {
                tvTopicLabel.text = selectedTopics.first().name
            } else {
                tvTopicLabel.text = "${selectedTopics.first().name} +${selectedTopics.size - 1}"
            }

            for (topic in selectedTopics) {
                val topicView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_selected_topic, selectedTopicsContainer, false) as TextView

                topicView.text = topic.name
                selectedTopicsContainer.addView(topicView)
            }
        }
    }

    private fun validatePost(): Boolean {
        if (etTitle.text.toString().trim().isEmpty()) {
            etTitle.error = "Vui lòng nhập tiêu đề"
            etTitle.requestFocus()
            return false
        }

        if (etContent.text.toString().trim().isEmpty()) {
            etContent.error = "Vui lòng nhập nội dung"
            etContent.requestFocus()
            return false
        }

        if (selectedTopics.isEmpty()) {
            Snackbar.make(requireView(), "Vui lòng chọn ít nhất một chủ đề", Snackbar.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun updatePost() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()
        val topicIds = selectedTopics.map { it.id }
        val userId = UserPreference.getUserId(requireContext())

        // Debug: In ra trạng thái ảnh hiện tại
        viewModel.debugImageState()

        // Lấy danh sách đường dẫn file của ảnh mới (chỉ ảnh mới thêm vào)
        val newImagePaths = viewModel.getNewImages().mapNotNull { uri ->
            val path = getPathFromUri(uri)
            Log.d("EditPostFragment", "New image path: $path")
            path
        }

        Log.d("EditPostFragment", "About to update post with:")
        Log.d("EditPostFragment", "- Current images: ${viewModel.getCurrentImages().size}")
        Log.d("EditPostFragment", "- New images: ${newImagePaths.size}")

        // Gọi ViewModel để cập nhật bài
        viewModel.updatePost(postId, title, content, userId, topicIds, newImagePaths)
    }

    private fun copyUriToTempFile(uri: Uri): String? {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                // Tạo tên file duy nhất
                val timeStamp = System.currentTimeMillis()
                val fileName = "temp_image_${timeStamp}.jpg"
                val tempFile = File(requireContext().cacheDir, fileName)

                // Copy dữ liệu từ URI vào file tạm
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()

                Log.d("EditPostFragment", "Created temp file: ${tempFile.absolutePath}, size: ${tempFile.length()}")
                return tempFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("EditPostFragment", "Error copying URI to temp file", e)
        }
        return null
    }

    private fun getPathFromUri(uri: Uri): String? {
        Log.d("EditPostFragment", "Converting URI to path: $uri")

        // Cách 1: Nếu là URI loại file, trả về đường dẫn trực tiếp
        if (uri.scheme == "file") {
            val path = uri.path
            Log.d("EditPostFragment", "File URI path: $path")
            return path
        }

        // Cách 2: Sử dụng ContentResolver để chuyển đổi URI thành file path
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                // Tạo file tạm thời
                val tempFile = File.createTempFile("temp_image_", ".jpg", requireContext().cacheDir)
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()

                val path = tempFile.absolutePath
                Log.d("EditPostFragment", "Created temp file: $path")
                return path
            }
        } catch (e: Exception) {
            Log.e("EditPostFragment", "Error creating temp file from URI", e)
        }

        // Cách 3: Thử dùng MediaStore (cho trường hợp picker cũ)
        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val path = it.getString(columnIndex)
                    Log.d("EditPostFragment", "MediaStore path: $path")
                    return path
                }
            }
        } catch (e: Exception) {
            Log.e("EditPostFragment", "Error getting path from MediaStore", e)
        }

        // Cách 4: Backup - copy file manually
        Log.d("EditPostFragment", "Trying backup method - copying URI to temp file")
        return copyUriToTempFile(uri)
    }
}