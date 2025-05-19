package com.example.app_music.presentation.feature.community.communityPostDetail

import android.Manifest
import android.app.Dialog
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
import com.example.app_music.domain.model.Post
import com.example.app_music.domain.utils.UrlUtils
import com.example.app_music.presentation.feature.community.adapter.CommentAdapter
import com.example.app_music.presentation.feature.community.communityPosting.SelectedImagesAdapter
import com.google.android.material.snackbar.Snackbar
import de.hdodenhof.circleimageview.CircleImageView

class PostDetailFragment : Fragment() {

    private lateinit var viewModel: PostDetailViewModel
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var selectedImagesAdapter: SelectedImagesAdapter
    private var currentUserId: Long = 0

    // Các thành phần UI
    private lateinit var tvPostTitle: TextView
    private lateinit var tvPostContent: TextView
    private lateinit var ivPostImage: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvTimeAgo: TextView
    private lateinit var ivUserAvatar: CircleImageView
    private lateinit var tvLikesCount: TextView
    private lateinit var tvCommentsCount: TextView
    private lateinit var recyclerViewComments: RecyclerView
    private lateinit var etAddComment: EditText
    private lateinit var btnAddImage: ImageButton
    private lateinit var btnSendComment: ImageButton
    private lateinit var topicTagsContainer: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var btnLike: LinearLayout
    private lateinit var ivLike: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var rvPostImages: RecyclerView
    private lateinit var tvImagesLabel: TextView
    private lateinit var rvSelectedCommentImages: RecyclerView
    private lateinit var tvCommentImageCount: TextView
    private lateinit var btnEdit: ImageButton

    private var currentPost: Post? = null
    private val imagesAdapter = PostImagesAdapter()
    private val maxCommentImageCount = 10

    // Biến theo dõi trạng thái thích cục bộ
    private var isLikedLocally = false

    // Chọn nhiều ảnh từ thư viện cho comment
    private val getMultipleContent = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            handleSelectedCommentImages(uris)
        }
    }

    // Yêu cầu quyền truy cập
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGalleryForComment()
        } else {
            Snackbar.make(requireView(), "Cần quyền truy cập hình ảnh để tải lên", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community_post_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[PostDetailViewModel::class.java]

        // Lấy ID người dùng hiện tại từ UserPreference
        currentUserId = UserPreference.getUserId(requireContext())

        // Tìm các view
        findViews(view)

        // Thiết lập RecyclerView & Adapter
        setupRecyclerViews()

        // Thiết lập các sự kiện click
        setupListeners()

        // Quan sát ViewModel
        observeViewModel()

        // Tải dữ liệu bài viết
        loadPostData()

        // Thiết lập post images adapter
        rvPostImages.adapter = imagesAdapter
        imagesAdapter.setOnImageClickListener { imageUrl, position ->
            showFullscreenImage(imageUrl)
        }
    }

    private fun findViews(view: View) {
        tvPostTitle = view.findViewById(R.id.tvPostTitle)
        tvPostContent = view.findViewById(R.id.tvPostContent)
        ivPostImage = view.findViewById(R.id.ivPostImage)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvTimeAgo = view.findViewById(R.id.tvTimeAgo)
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar)
        tvLikesCount = view.findViewById(R.id.tvLikesCount)
        tvCommentsCount = view.findViewById(R.id.tvCommentsCount)
        recyclerViewComments = view.findViewById(R.id.recyclerViewComments)
        etAddComment = view.findViewById(R.id.etAddComment)
        btnAddImage = view.findViewById(R.id.btnAddImage)
        btnSendComment = view.findViewById(R.id.btnSendComment)
        topicTagsContainer = view.findViewById(R.id.topicTagsContainer)
        btnBack = view.findViewById(R.id.btnBack)
        btnLike = view.findViewById(R.id.btnLike)
        ivLike = view.findViewById(R.id.ivLike)
        rvPostImages = view.findViewById(R.id.rvPostImages)
        tvImagesLabel = view.findViewById(R.id.tvImagesLabel)
        btnEdit = view.findViewById(R.id.btnEdit)

        // Tìm hoặc tạo các view cho comment images
        rvSelectedCommentImages = view.findViewById(R.id.rvSelectedCommentImages) ?: run {
            // Nếu không có trong layout, tạo mới và thêm vào
            val recyclerView = RecyclerView(requireContext())
            recyclerView.id = View.generateViewId()
            val commentInputArea = view.findViewById<LinearLayout>(R.id.commentInputArea)

            val imageCountText = TextView(requireContext()).apply {
                id = View.generateViewId()
                text = "0/$maxCommentImageCount"
                textSize = 12f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                visibility = View.GONE
            }
            tvCommentImageCount = imageCountText

            // Thêm vào comment input area (trước LinearLayout controls)
            commentInputArea.addView(imageCountText, commentInputArea.childCount - 1)
            commentInputArea.addView(recyclerView, commentInputArea.childCount - 1)

            recyclerView
        }

        tvCommentImageCount = view.findViewById<TextView?>(R.id.tvCommentImageCount) ?: run {
            val textView = TextView(requireContext())
            textView.text = "0/$maxCommentImageCount"
            textView.textSize = 12f
            textView.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            textView.visibility = View.GONE
            textView
        }

        // Thêm progress bar nếu chưa có
        progressBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleLarge)
        (view as ViewGroup).addView(progressBar)
        val params = progressBar.layoutParams as ViewGroup.LayoutParams
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        progressBar.layoutParams = params
        progressBar.visibility = View.GONE
    }

    private fun setupRecyclerViews() {
        // Comment adapter
        commentAdapter = CommentAdapter()
        recyclerViewComments.adapter = commentAdapter

        // Thiết lập sự kiện cho comment adapter
        commentAdapter.setOnCommentReplyListener { comment ->
            // Điều hướng đến reply fragment với commentId
            val bundle = Bundle().apply {
                putLong("commentId", comment.id)
            }
            findNavController().navigate(R.id.action_postDetailFragment_to_commentReplyFragment, bundle)
        }

        commentAdapter.setOnViewRepliesListener { comment ->
            // Điều hướng đến reply fragment để xem replies
            val bundle = Bundle().apply {
                putLong("commentId", comment.id)
            }
            findNavController().navigate(R.id.action_postDetailFragment_to_commentReplyFragment, bundle)
        }

        // Selected comment images adapter
        selectedImagesAdapter = SelectedImagesAdapter()
        rvSelectedCommentImages.layoutManager = GridLayoutManager(requireContext(), 3)
        rvSelectedCommentImages.adapter = selectedImagesAdapter

        // Xử lý sự kiện xóa ảnh comment
        selectedImagesAdapter.setOnImageRemoveListener { position ->
            viewModel.removeCommentImage(position)
        }
    }

    private fun setupListeners() {
        // Nút quay lại
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Nút thích
        btnLike.setOnClickListener {
            // Đảo ngược trạng thái thích cục bộ
            isLikedLocally = !isLikedLocally

            // Cập nhật UI ngay lập tức
            updateLikeUI(isLikedLocally)

            // Gọi viewModel để xử lý thao tác thích/bỏ thích trên server
            viewModel.toggleLikePost(currentUserId)
        }

        // Thêm ảnh vào comment
        btnAddImage.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        // Gửi comment
        btnSendComment.setOnClickListener {
            val commentText = etAddComment.text.toString().trim()
            val selectedImages = viewModel.commentImageUris.value ?: emptyList()

            if (commentText.isNotEmpty() || selectedImages.isNotEmpty()) {
                // Chuyển đổi URI thành đường dẫn file
                val imagePaths = selectedImages.mapNotNull { uri ->
                    getPathFromUri(uri)
                }

                viewModel.postComment(commentText, currentUserId, imagePaths)
                etAddComment.text.clear()
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập nội dung hoặc chọn ảnh", Toast.LENGTH_SHORT).show()
            }
        }

        btnEdit.setOnClickListener {
            currentPost?.let { post ->
                // Điều hướng đến EditPostFragment
                val bundle = Bundle().apply {
                    putLong("postId", post.id)
                }
                findNavController().navigate(R.id.action_postDetailFragment_to_editPostFragment, bundle)
            }
        }
    }

    private fun observeViewModel() {
        // Quan sát dữ liệu bài viết
        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let {
                updateUI(it)
            }
        }

        // Quan sát danh sách comments
        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            commentAdapter.submitList(comments)
            tvCommentsCount.text = "${comments.size} bình luận"
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

        // Quan sát ảnh comment
        viewModel.commentImageUris.observe(viewLifecycleOwner) { uris ->
            selectedImagesAdapter.submitList(uris)
            updateCommentImageUI(uris)
        }

        // Quan sát kết quả đăng comment
        viewModel.commentResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Đăng bình luận thành công", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPostData() {
        // Lấy postId từ arguments bundle
        val postId = arguments?.getLong("postId") ?: -1L
        if (postId == -1L) {
            Snackbar.make(requireView(), "ID bài viết không hợp lệ", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }
        viewModel.loadPostDetails(postId)
    }

    private fun updateUI(post: Post) {
        // Hiển thị thông tin bài viết
        tvPostTitle.text = post.title
        tvPostContent.text = post.content
        tvUserName.text = post.user.username
        tvTimeAgo.text = tinhThoiGianTruocDay(post.createDate)
        tvLikesCount.text = "${post.react.size} lượt thích"
        currentPost = post

        val isOwner = post.user.id == currentUserId
        btnEdit.visibility = if (isOwner) View.VISIBLE else View.GONE

        // Kiểm tra xem người dùng hiện tại đã thích bài viết này chưa
        val hasUserLiked = post.react.any { it.user.id == currentUserId }

        // Cập nhật trạng thái thích cục bộ và UI
        isLikedLocally = hasUserLiked
        updateLikeUI(isLikedLocally)

        // Tải avatar người dùng với URL tuyệt đối
        val avatarUrl = UrlUtils.getAbsoluteUrl(post.user.avatarUrl)
        if (avatarUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(avatarUrl)
                .placeholder(R.drawable.avatar)
                .into(ivUserAvatar)
        }

        val allImages = post.getAllImages()
        if (allImages.isNotEmpty()) {
            // Hiển thị RecyclerView và label
            rvPostImages.visibility = View.VISIBLE
            tvImagesLabel.visibility = View.VISIBLE

            // Đặt danh sách ảnh cho adapter
            imagesAdapter.submitList(allImages)

            // Ẩn ảnh chính nếu đã hiển thị tất cả trong gallery
            ivPostImage.visibility = View.GONE
        } else {
            // Ẩn RecyclerView và label nếu không có ảnh
            rvPostImages.visibility = View.GONE
            tvImagesLabel.visibility = View.GONE

            // Kiểm tra xem có ảnh chính không để hiển thị
            val mainImageUrl = UrlUtils.getAbsoluteUrl(post.image)
            if (mainImageUrl.isNotEmpty()) {
                ivPostImage.visibility = View.VISIBLE
                Glide.with(requireContext())
                    .load(mainImageUrl)
                    .into(ivPostImage)
            } else {
                ivPostImage.visibility = View.GONE
            }
        }

        // Hiển thị các tag chủ đề
        setupTopicTags(post)
    }

    // Thêm phương thức này để cập nhật UI trái tim thích
    private fun updateLikeUI(isLiked: Boolean) {
        if (isLiked) {
            // Đã thích - trái tim đỏ
            ivLike.setImageResource(R.drawable.ic_liked_red)
        } else {
            // Chưa thích - trái tim mặc định
            ivLike.setImageResource(R.drawable.ic_liked)
        }
    }

    private fun setupTopicTags(post: Post) {
        // Xóa tất cả các tag hiện tại
        topicTagsContainer.removeAllViews()

        // Kiểm tra danh sách topics
        if (post.topics.isNotEmpty()) {
            // Lặp qua tất cả các topic và tạo tag view cho mỗi topic
            post.topics.forEach { topic ->
                // Inflate layout từ item_tag.xml
                val tagView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.tag_item, topicTagsContainer, false) as TextView

                // Đặt tên topic vào tag
                tagView.text = topic.name

                // Thêm tag vào container
                topicTagsContainer.addView(tagView)
            }
        } else {
            // Nếu không có topic nào, thêm một tag mặc định
            val defaultTag = LayoutInflater.from(requireContext())
                .inflate(R.layout.tag_item, topicTagsContainer, false) as TextView
            defaultTag.text = "General"
            topicTagsContainer.addView(defaultTag)
        }

        // Đảm bảo container tags luôn hiển thị
        topicTagsContainer.visibility = View.VISIBLE
    }

    private fun checkPermissionAndOpenGallery() {
        val currentImageCount = viewModel.commentImageUris.value?.size ?: 0
        if (currentImageCount >= maxCommentImageCount) {
            Toast.makeText(requireContext(), "Chỉ được chọn tối đa $maxCommentImageCount ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openGalleryForComment()
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

    private fun openGalleryForComment() {
        val currentImageCount = viewModel.commentImageUris.value?.size ?: 0
        val remainingSlots = maxCommentImageCount - currentImageCount

        if (remainingSlots <= 0) {
            Toast.makeText(requireContext(), "Đã đạt giới hạn số lượng ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        getMultipleContent.launch("image/*")
    }

    private fun handleSelectedCommentImages(uris: List<Uri>) {
        val currentImages = viewModel.commentImageUris.value ?: emptyList()
        val currentCount = currentImages.size
        val newCount = currentCount + uris.size

        if (newCount > maxCommentImageCount) {
            val canAdd = maxCommentImageCount - currentCount
            val message = "Chỉ có thể thêm $canAdd ảnh nữa. Đã đạt giới hạn $maxCommentImageCount ảnh."
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

            val limitedUris = uris.take(canAdd)
            viewModel.addCommentImages(limitedUris)
        } else {
            viewModel.addCommentImages(uris)
        }
    }

    private fun updateCommentImageUI(uris: List<Uri>) {
        val count = uris.size
        tvCommentImageCount.text = "$count/$maxCommentImageCount"

        if (count >= maxCommentImageCount) {
            btnAddImage.alpha = 0.5f
            tvCommentImageCount.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
        } else {
            btnAddImage.alpha = 1.0f
            tvCommentImageCount.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        if (count > 0) {
            rvSelectedCommentImages.visibility = View.VISIBLE
            tvCommentImageCount.visibility = View.VISIBLE
        } else {
            rvSelectedCommentImages.visibility = View.GONE
            tvCommentImageCount.visibility = View.GONE
        }
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

    private fun tinhThoiGianTruocDay(ngay: java.time.LocalDate): String {
        val ngayHienTai = java.time.LocalDate.now()
        val soNgay = java.time.Period.between(ngay, ngayHienTai).days

        return when {
            soNgay == 0 -> "Hôm nay"
            soNgay == 1 -> "Hôm qua"
            soNgay < 7 -> "$soNgay ngày trước"
            soNgay < 30 -> "${soNgay / 7} tuần trước"
            soNgay < 365 -> "${soNgay / 30} tháng trước"
            else -> "${soNgay / 365} năm trước"
        }
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