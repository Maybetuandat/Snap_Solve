package com.example.app_music.presentation.feature.community.communityPostDetail

import android.Manifest
import android.app.Activity
import android.app.Dialog
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
import com.bumptech.glide.Glide
import com.example.app_music.R
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.domain.model.Comment
import com.example.app_music.domain.utils.UrlUtils
import com.example.app_music.presentation.feature.community.adapter.CommentAdapter
import com.example.app_music.presentation.feature.community.adapter.CommentImagesAdapter
import com.example.app_music.presentation.feature.community.communityPosting.SelectedImagesAdapter
import com.google.android.material.snackbar.Snackbar
import de.hdodenhof.circleimageview.CircleImageView

class CommentReplyFragment : Fragment() {

    private lateinit var viewModel: CommentReplyViewModel
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var selectedImagesAdapter: SelectedImagesAdapter

    // UI Components
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var ivParentUserAvatar: CircleImageView
    private lateinit var tvParentUserName: TextView
    private lateinit var tvParentTimeAgo: TextView
    private lateinit var tvParentContent: TextView
    private lateinit var rvParentImages: RecyclerView
    private lateinit var rvReplies: RecyclerView
    private lateinit var etReplyContent: EditText
    private lateinit var btnAddImage: ImageButton
    private lateinit var btnSendReply: ImageButton
    private lateinit var rvSelectedImages: RecyclerView
    private lateinit var tvImageCount: TextView
    private lateinit var progressBar: ProgressBar

    private var parentComment: Comment? = null
    private val maxImageCount = 10
    private val commentImagesAdapter = CommentImagesAdapter()


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
            Snackbar.make(requireView(), "Cần quyền truy cập hình ảnh để đăng ảnh", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_comment_reply, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[CommentReplyViewModel::class.java]

        // Tìm các view
        findViews(view)

        // Thiết lập RecyclerViews
        setupRecyclerViews()

        // Thiết lập các sự kiện
        setupListeners()

        // Quan sát ViewModel
        observeViewModel()

        // Tải dữ liệu
        loadCommentData()

        commentImagesAdapter.setOnImageClickListener { imageUrl, position ->
            showFullscreenImage(imageUrl)
        }
    }

    private fun findViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        tvTitle = view.findViewById(R.id.tvTitle)
        ivParentUserAvatar = view.findViewById(R.id.ivParentUserAvatar)
        tvParentUserName = view.findViewById(R.id.tvParentUserName)
        tvParentTimeAgo = view.findViewById(R.id.tvParentTimeAgo)
        tvParentContent = view.findViewById(R.id.tvParentContent)
        rvParentImages = view.findViewById(R.id.rvParentImages)
        rvReplies = view.findViewById(R.id.rvReplies)
        etReplyContent = view.findViewById(R.id.etReplyContent)
        btnAddImage = view.findViewById(R.id.btnAddImage)
        btnSendReply = view.findViewById(R.id.btnSendReply)
        rvSelectedImages = view.findViewById(R.id.rvSelectedImages)
        tvImageCount = view.findViewById(R.id.tvImageCount)

        // Thêm progress bar
        progressBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleLarge)
        (view as ViewGroup).addView(progressBar)
        val params = progressBar.layoutParams as ViewGroup.LayoutParams
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        progressBar.layoutParams = params
        progressBar.visibility = View.GONE
    }

    private fun setupRecyclerViews() {
        // Adapter cho replies
        commentAdapter = CommentAdapter()
        rvReplies.adapter = commentAdapter

        // Adapter cho ảnh đã chọn
        selectedImagesAdapter = SelectedImagesAdapter()
        rvSelectedImages.layoutManager = GridLayoutManager(requireContext(), 3)
        rvSelectedImages.adapter = selectedImagesAdapter

        // Xử lý sự kiện xóa ảnh
        selectedImagesAdapter.setOnImageRemoveListener { position ->
            selectedImagesAdapter.removeImage(position)
            updateImageCounter()
        }
    }

    private fun setupListeners() {
        // Nút quay lại
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Thêm ảnh
        btnAddImage.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        // Gửi reply
        btnSendReply.setOnClickListener {
            sendReply()
        }
    }

    private fun observeViewModel() {
        // Quan sát parent comment
        viewModel.parentComment.observe(viewLifecycleOwner) { comment ->
            comment?.let {
                parentComment = it
                updateParentCommentUI(it)
            }
        }

        // Quan sát danh sách replies
        viewModel.replies.observe(viewLifecycleOwner) { replies ->
            commentAdapter.submitList(replies)
        }

        // Quan sát trạng thái tải
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Quan sát lỗi
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG).show()
            }
        }

        // Quan sát kết quả gửi reply
        viewModel.replyResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                // Xóa nội dung sau khi gửi thành công
                etReplyContent.text.clear()
                selectedImagesAdapter.submitList(emptyList())
                updateImageCounter()

                Toast.makeText(requireContext(), "Gửi trả lời thành công", Toast.LENGTH_SHORT).show()

                // Tải lại danh sách replies
                parentComment?.let {
                    viewModel.loadReplies(it.id)
                }
            }
        }
    }

    private fun loadCommentData() {
        // Lấy commentId từ arguments
        val commentId = arguments?.getLong("commentId") ?: -1L
        if (commentId == -1L) {
            Snackbar.make(requireView(), "ID comment không hợp lệ", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        // Tải thông tin comment và replies
        viewModel.loadCommentWithReplies(commentId)
    }

    private fun updateParentCommentUI(comment: Comment) {
        tvParentUserName.text = comment.user.username
        tvParentTimeAgo.text = comment.getTimeAgo()
        tvParentContent.text = comment.content

        // Tải avatar
        val avatarUrl = UrlUtils.getAbsoluteUrl(comment.user.avatarUrl)
        if (avatarUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(avatarUrl)
                .placeholder(R.drawable.avatar)
                .into(ivParentUserAvatar)
        }

        // Hiển thị ảnh của comment nếu có
        if (comment.images.isNotEmpty()) {
            rvParentImages.visibility = View.VISIBLE
            rvParentImages.layoutManager = GridLayoutManager(requireContext(), 2)
            rvParentImages.adapter = commentImagesAdapter
            commentImagesAdapter.submitList(comment.images)
        } else {
            rvParentImages.visibility = View.GONE
        }

    }

    private fun checkPermissionAndOpenGallery() {
        if (selectedImagesAdapter.getImages().size >= maxImageCount) {
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
                Snackbar.make(requireView(), "Cần quyền truy cập hình ảnh để đăng ảnh", Snackbar.LENGTH_LONG)
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
        val remainingSlots = maxImageCount - selectedImagesAdapter.getImages().size
        if (remainingSlots <= 0) {
            Toast.makeText(requireContext(), "Đã đạt giới hạn số lượng ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        getMultipleContent.launch("image/*")
    }

    private fun handleSelectedImages(uris: List<Uri>) {
        val currentCount = selectedImagesAdapter.getImages().size
        val newCount = currentCount + uris.size

        if (newCount > maxImageCount) {
            val canAdd = maxImageCount - currentCount
            val message = "Chỉ có thể thêm $canAdd ảnh nữa. Đã đạt giới hạn $maxImageCount ảnh."
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

            val limitedUris = uris.take(canAdd)
            addImagesToAdapter(limitedUris)
        } else {
            addImagesToAdapter(uris)
        }
    }

    private fun addImagesToAdapter(uris: List<Uri>) {
        for (uri in uris) {
            selectedImagesAdapter.addImage(uri)
        }
        updateImageCounter()

        if (selectedImagesAdapter.getImages().isNotEmpty()) {
            rvSelectedImages.visibility = View.VISIBLE
        }
    }

    private fun updateImageCounter() {
        val count = selectedImagesAdapter.getImages().size
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
        } else {
            rvSelectedImages.visibility = View.GONE
        }
    }

    private fun sendReply() {
        val content = etReplyContent.text.toString().trim()
        if (content.isEmpty() && selectedImagesAdapter.getImages().isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập nội dung hoặc chọn ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        val parentCommentId = parentComment?.id ?: return
        val userId = UserPreference.getUserId(requireContext())

        // Lấy danh sách đường dẫn ảnh
        val imagePaths = selectedImagesAdapter.getImages().mapNotNull { uri ->
            getPathFromUri(uri)
        }

        // Gửi reply
        viewModel.createReply(content, userId, parentCommentId, imagePaths)
    }

    private fun getPathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }

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

    private fun showFullscreenImage(imageUrl: String) {
        // Tạo một dialog hiển thị ảnh toàn màn hình
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(requireContext())

        // Thiết lập thuộc tính cho ImageView
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        // Tải ảnh vào ImageView
        Glide.with(requireContext())
            .load(imageUrl)
            .into(imageView)

        // Thiết lập sự kiện click để đóng dialog
        imageView.setOnClickListener {
            dialog.dismiss()
        }

        // Hiển thị dialog
        dialog.setContentView(imageView)
        dialog.show()
    }
}