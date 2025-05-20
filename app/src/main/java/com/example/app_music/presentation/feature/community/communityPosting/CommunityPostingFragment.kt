package com.example.app_music.presentation.feature.community.communityPosting

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.snackbar.Snackbar
import java.io.File

class CommunityPostingFragment : Fragment() {
    private lateinit var viewModel: CommunityPostingViewModel

    private lateinit var btnClose: ImageButton
    private lateinit var btnPost: TextView
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
    private val maxImageCount = 10 // Số lượng ảnh tối đa

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
        return inflater.inflate(R.layout.fragment_community_posting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[CommunityPostingViewModel::class.java]

        // Tìm các view
        findViews(view)

        // Thiết lập RecyclerView cho ảnh đã chọn
        setupImagesRecyclerView()

        // Thiết lập các sự kiện
        setupListeners()

        // Quan sát ViewModel
        observeViewModel()

        // Tải danh sách topic từ API
        viewModel.loadTopics()
    }

    private fun findViews(view: View) {
        btnClose = view.findViewById(R.id.btnClose)
        btnPost = view.findViewById(R.id.btnPost)
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
            imageAdapter.removeImage(position)
            updateImageCounter()
        }
    }

    private fun setupListeners() {
        // Nút đóng - quay lại
        btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        // Nút đăng bài - kiểm tra và đăng bài
        btnPost.setOnClickListener {
            if (validatePost()) {
                publishPost()
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
        // Quan sát danh sách topic
        viewModel.topics.observe(viewLifecycleOwner) { topics ->
            // Lưu lại danh sách topic để sử dụng khi hiển thị dialog
            if (topics.isNotEmpty()) {
                // Nếu chưa có topic nào được chọn, có thể đặt mặc định topic đầu tiên
                if (selectedTopics.isEmpty()) {
                    // Nếu không muốn chọn mặc định, hãy comment dòng này
                    // selectedTopics.add(topics[0])
                    // updateSelectedTopicsUI()
                }
            }
        }

        // Quan sát trạng thái đăng bài
        viewModel.isPosting.observe(viewLifecycleOwner) { isPosting ->
            progressBar.visibility = if (isPosting) View.VISIBLE else View.GONE
            btnPost.isEnabled = !isPosting
            btnAddImage.isEnabled = !isPosting
            topicSelector.isEnabled = !isPosting
        }

        // Quan sát kết quả đăng bài
        viewModel.postResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Đăng bài thành công", Toast.LENGTH_SHORT).show()
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

    private fun checkPermissionAndOpenGallery() {
        // Kiểm tra quyền truy cập thư viện ảnh
        if (imageAdapter.getImages().size >= maxImageCount) {
            Toast.makeText(requireContext(), "Chỉ được chọn tối đa $maxImageCount ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra quyền dựa trên phiên bản Android
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        // Kiểm tra xem đã có quyền chưa
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // Giải thích tại sao cần quyền
                Snackbar.make(requireView(), "Cần quyền truy cập hình ảnh để đăng bài với ảnh", Snackbar.LENGTH_LONG)
                    .setAction("Cấp quyền") {
                        requestPermissionLauncher.launch(permission)
                    }
                    .show()
            }
            else -> {
                // Yêu cầu quyền
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallery() {
        // Tính toán số lượng ảnh còn có thể chọn
        val remainingSlots = maxImageCount - imageAdapter.getImages().size

        if (remainingSlots <= 0) {
            Toast.makeText(requireContext(), "Đã đạt giới hạn số lượng ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        // Mở thư viện ảnh để chọn nhiều ảnh
        getMultipleContent.launch("image/*")
    }

    private fun handleSelectedImages(uris: List<Uri>) {
        // Kiểm tra số lượng hình ảnh được chọn
        val currentCount = imageAdapter.getImages().size
        val newCount = currentCount + uris.size

        if (newCount > maxImageCount) {
            // Thông báo và chỉ lấy số lượng ảnh có thể thêm vào
            val canAdd = maxImageCount - currentCount
            val message = "Chỉ có thể thêm $canAdd ảnh nữa. Đã đạt giới hạn $maxImageCount ảnh."
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

            // Chỉ lấy những ảnh trong giới hạn
            val limitedUris = uris.take(canAdd)
            addImagesToAdapter(limitedUris)
        } else {
            // Thêm tất cả ảnh được chọn
            addImagesToAdapter(uris)
        }
    }

    private fun addImagesToAdapter(uris: List<Uri>) {
        // Thêm từng URI vào adapter
        for (uri in uris) {
            imageAdapter.addImage(uri)
        }

        // Cập nhật UI hiển thị số lượng ảnh
        updateImageCounter()

        // Hiển thị RecyclerView và tiêu đề nếu có ảnh
        if (imageAdapter.getImages().isNotEmpty()) {
            rvSelectedImages.visibility = View.VISIBLE
            tvSelectedImagesTitle.visibility = View.VISIBLE
        }
    }

    private fun updateImageCounter() {
        val count = imageAdapter.getImages().size
        tvImageCount.text = "$count/$maxImageCount"

        // Hiển thị mờ nút thêm ảnh khi đạt giới hạn
        if (count >= maxImageCount) {
            btnAddImage.alpha = 0.5f
            tvImageCount.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
        } else {
            btnAddImage.alpha = 1.0f
            tvImageCount.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        // Ẩn hoặc hiển thị RecyclerView và tiêu đề
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
                // Cập nhật danh sách topic đã chọn
                this@CommunityPostingFragment.selectedTopics.clear()
                this@CommunityPostingFragment.selectedTopics.addAll(selectedTopics)

                // Cập nhật UI
                updateSelectedTopicsUI()
            }
        })

        // Hiển thị dialog
        topicBottomSheet.show(parentFragmentManager, TopicSelectionBottomSheet.TAG)
    }

    private fun updateSelectedTopicsUI() {
        // Xóa tất cả các tag topic hiện tại
        selectedTopicsContainer.removeAllViews()

        // Thiết lập hiển thị của container
        if (selectedTopics.isEmpty()) {
            tvSelectedTopicsTitle.visibility = View.GONE
            selectedTopicsContainer.visibility = View.GONE
            tvTopicLabel.text = getString(R.string.posting_choose_topic)
        } else {
            tvSelectedTopicsTitle.visibility = View.VISIBLE
            selectedTopicsContainer.visibility = View.VISIBLE

            // Cập nhật text ở phần chọn topic
            if (selectedTopics.size == 1) {
                tvTopicLabel.text = selectedTopics.first().name
            } else {
                tvTopicLabel.text = "${selectedTopics.first().name} +${selectedTopics.size - 1}"
            }

            // Thêm tag cho mỗi topic đã chọn
            for (topic in selectedTopics) {
                val topicView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_selected_topic, selectedTopicsContainer, false) as TextView

                topicView.text = topic.name

                // Thêm vào container
                selectedTopicsContainer.addView(topicView)
            }
        }
    }

    private fun validatePost(): Boolean {
        // Kiểm tra tiêu đề
        if (etTitle.text.toString().trim().isEmpty()) {
            etTitle.error = "Vui lòng nhập tiêu đề"
            etTitle.requestFocus()
            return false
        }

        // Kiểm tra nội dung
        if (etContent.text.toString().trim().isEmpty()) {
            etContent.error = "Vui lòng nhập nội dung"
            etContent.requestFocus()
            return false
        }

        // Kiểm tra topic
        if (selectedTopics.isEmpty()) {
            Snackbar.make(requireView(), "Vui lòng chọn ít nhất một chủ đề", Snackbar.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun publishPost() {
        // Lấy dữ liệu từ form
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()
        val topicIds = selectedTopics.map { it.id }
        val userId = UserPreference.getUserId(requireContext())

        // Lấy danh sách đường dẫn tệp ảnh từ URI
        val imagePaths = imageAdapter.getImages().mapNotNull { uri ->
            // Chuyển đổi URI thành đường dẫn tệp tin
            getPathFromUri(uri)
        }

        // Gọi ViewModel để đăng bài
        viewModel.createPost(title, content, userId, topicIds, imagePaths)
    }

    private fun getPathFromUri(uri: Uri): String? {
        // Nếu là URI loại file, trả về đường dẫn trực tiếp
        if (uri.scheme == "file") {
            return uri.path
        }

        // Nếu là URI loại content, truy vấn MediaStore để lấy đường dẫn thực
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return it.getString(columnIndex)
            }
        }

        return null
    }
}