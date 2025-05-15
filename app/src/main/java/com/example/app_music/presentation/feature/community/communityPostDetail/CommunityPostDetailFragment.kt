package com.example.app_music.presentation.feature.community.communityPostDetail

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app_music.R
import com.example.app_music.data.model.Post
import com.example.app_music.presentation.feature.community.adapter.CommentAdapter
import com.google.android.material.snackbar.Snackbar
import de.hdodenhof.circleimageview.CircleImageView

class PostDetailFragment : Fragment() {

    private lateinit var viewModel: PostDetailViewModel
    private lateinit var commentAdapter: CommentAdapter

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
    private lateinit var progressBar: ProgressBar

    // Chọn ảnh từ thư viện
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                viewModel.setCommentImage(imageUri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Sử dụng layout mới đã được điều chỉnh
        return inflater.inflate(R.layout.fragment_community_post_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[PostDetailViewModel::class.java]

        // Tìm các view
        findViews(view)

        // Thiết lập RecyclerView & Adapter
        setupRecyclerView()

        // Thiết lập các sự kiện click
        setupListeners()

        // Quan sát ViewModel
        observeViewModel()

        // Tải dữ liệu bài viết
        loadPostData()
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

        // Thêm progress bar nếu chưa có
        progressBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleLarge)
        (view as ViewGroup).addView(progressBar)
        val params = progressBar.layoutParams as ViewGroup.LayoutParams
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        progressBar.layoutParams = params
        progressBar.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter()
        recyclerViewComments.adapter = commentAdapter

        // Thiết lập sự kiện tương tác với comment
        commentAdapter.setOnCommentLikeListener { comment ->
            viewModel.likeComment(comment)
        }

        commentAdapter.setOnCommentReplyListener { comment ->
            etAddComment.requestFocus()
            etAddComment.setText("@${comment.user.username} ")
            etAddComment.setSelection(etAddComment.text.length)
        }
    }

    private fun setupListeners() {
        // Nút quay lại
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Nút thích
        btnLike.setOnClickListener {
            viewModel.likePost()
        }

        // Thêm ảnh vào comment
        btnAddImage.setOnClickListener {
            openGallery()
        }

        // Gửi comment
        btnSendComment.setOnClickListener {
            val commentText = etAddComment.text.toString().trim()
            viewModel.postComment(commentText, viewModel.commentImageUri.value)
            etAddComment.text.clear()
        }
    }

    private fun observeViewModel() {
        // Quan sát dữ liệu bài viết
        viewModel.post.observe(viewLifecycleOwner) { post ->
            updateUI(post)
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
        viewModel.commentImageUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                Snackbar.make(requireView(), "Đã đính kèm ảnh", Snackbar.LENGTH_SHORT).show()
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

        // Tải avatar người dùng
        if (!post.user.avatarUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(post.user.avatarUrl)
                .placeholder(R.drawable.avatar)
                .into(ivUserAvatar)
        }

        // Tải ảnh bài viết nếu có
        if (!post.image.isNullOrEmpty()) {
            ivPostImage.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(post.image)
                .into(ivPostImage)
        } else {
            ivPostImage.visibility = View.GONE
        }

        // Hiển thị số lượng comments
        tvCommentsCount.text = "${post.comment.size} bình luận"

        // Hiển thị các tag chủ đề
        setupTopicTags(post)
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

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
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
}