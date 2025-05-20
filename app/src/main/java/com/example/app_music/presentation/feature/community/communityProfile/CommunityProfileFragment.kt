package com.example.app_music.presentation.feature.community.communityProfile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app_music.R
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.presentation.feature.community.adapter.PostAdapter
import com.google.android.material.tabs.TabLayout
import de.hdodenhof.circleimageview.CircleImageView

class CommunityProfileFragment : Fragment() {

    private lateinit var viewModel: CommunityProfileViewModel
    private lateinit var postAdapter: PostAdapter

    private lateinit var recyclerViewYourPosts: RecyclerView
    private lateinit var recyclerViewLikedPosts: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var ivUserAvatar: CircleImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserStatus: TextView

    private var currentUserId: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[CommunityProfileViewModel::class.java]

        // Lấy ID người dùng hiện tại
        currentUserId = UserPreference.getUserId(requireContext())

        // Tìm các view
        findViews(view)

        // Thiết lập các adapter và sự kiện
        setupRecyclerViews()
        setupListeners()

        // Quan sát ViewModel
        observeViewModel()

        // Tải dữ liệu
        loadData()
    }

    private fun findViews(view: View) {
        recyclerViewYourPosts = view.findViewById(R.id.recyclerViewYourPosts)
        recyclerViewLikedPosts = view.findViewById(R.id.recyclerViewLikedPosts)
        tabLayout = view.findViewById(R.id.tabLayout)
        btnBack = view.findViewById(R.id.btnBack)
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserStatus = view.findViewById(R.id.tvUserStatus)

        // Thêm ProgressBar vào view nếu chưa có
        progressBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleLarge)
        (view as ViewGroup).addView(progressBar)
        val params = progressBar.layoutParams as ViewGroup.LayoutParams
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        progressBar.layoutParams = params
        progressBar.visibility = View.GONE
    }

    private fun setupRecyclerViews() {
        // Tạo adapter cho cả hai RecyclerView
        val yourPostsAdapter = PostAdapter()
        val likedPostsAdapter = PostAdapter()

        // Đặt ID người dùng hiện tại cho adapter
        yourPostsAdapter.setCurrentUserId(currentUserId)
        likedPostsAdapter.setCurrentUserId(currentUserId)

        // Gán adapter cho RecyclerView
        recyclerViewYourPosts.adapter = yourPostsAdapter
        recyclerViewLikedPosts.adapter = likedPostsAdapter

        // Thiết lập sự kiện click cho các bài đăng
        yourPostsAdapter.setOnPostClickListener { post ->
            navigateToPostDetail(post.id)
        }

        likedPostsAdapter.setOnPostClickListener { post ->
            navigateToPostDetail(post.id)
        }

        // Thiết lập sự kiện thích/bỏ thích
        yourPostsAdapter.setOnLikeClickListener { post ->
            // Xử lý thích/bỏ thích bài viết (có thể thêm code sau)
        }

        likedPostsAdapter.setOnLikeClickListener { post ->
            // Xử lý thích/bỏ thích bài viết (có thể thêm code sau)
        }
    }

    private fun setupListeners() {
        // Xử lý nút Back
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Xử lý sự kiện chọn tab
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Tab "Your posts"
                        recyclerViewYourPosts.visibility = View.VISIBLE
                        recyclerViewLikedPosts.visibility = View.GONE
                    }
                    1 -> { // Tab "Liked posts"
                        recyclerViewYourPosts.visibility = View.GONE
                        recyclerViewLikedPosts.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeViewModel() {
        // Quan sát thông tin người dùng
        viewModel.userInfo.observe(viewLifecycleOwner) { user ->
            user?.let {
                tvUserName.text = it.username
                tvUserStatus.text = it.statusMessage ?: "Không có giới thiệu"

                // Tải ảnh đại diện
                if (!it.avatarUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(it.avatarUrl)
                        .placeholder(R.drawable.avatar)
                        .into(ivUserAvatar)
                }
            }
        }

        // Quan sát danh sách bài đăng của người dùng
        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            (recyclerViewYourPosts.adapter as PostAdapter).submitList(posts)
        }

        // Quan sát danh sách bài đăng đã thích
        viewModel.likedPosts.observe(viewLifecycleOwner) { posts ->
            (recyclerViewLikedPosts.adapter as PostAdapter).submitList(posts)
        }

        // Quan sát trạng thái tải
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Quan sát lỗi
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadData() {
        viewModel.loadUserInfo(currentUserId)
        viewModel.loadUserPosts(currentUserId)
        viewModel.loadLikedPosts(currentUserId)
    }

    private fun navigateToPostDetail(postId: Long) {
        val bundle = Bundle().apply {
            putLong("postId", postId)
        }
        findNavController().navigate(R.id.action_communityProfileFragment_to_postDetailFragment, bundle)
    }
}