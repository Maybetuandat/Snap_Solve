package com.example.app_music.presentation.feature.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.app_music.R
import com.example.app_music.presentation.feature.community.adapter.PostAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CommunityFragment : Fragment() {

    private lateinit var viewModel: CommunityViewModel
    private lateinit var postAdapter: PostAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var radioGroup: RadioGroup
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]

        // Tìm các view
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar) // Thêm ProgressBar vào layout
        radioGroup = view.findViewById(R.id.radioGroup)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh) // Thêm SwipeRefreshLayout vào layout

        // Thiết lập adapter
        postAdapter = PostAdapter()
        recyclerView.adapter = postAdapter

        // Thiết lập sự kiện
        setupListeners(view)

        // Observe ViewModel
        observeViewModel()

        // Tải dữ liệu
        viewModel.loadLatestPosts()
    }

    private fun setupListeners(view: View) {
        // Nút tìm kiếm
        val searchButton = view.findViewById<ImageButton>(R.id.btnSearch)
        searchButton.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_searchPostFragment)
        }

        // Nút profile
        val profileButton = view.findViewById<ImageButton>(R.id.btnProfile)
        profileButton.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_communityProfileFragment)
        }

        // Nút tạo bài viết mới
        val fabCreate = view.findViewById<FloatingActionButton>(R.id.fabCreate)
        fabCreate.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_communityPostingFragment)
        }

        // Xử lý sự kiện khi chọn tab
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbAll -> viewModel.loadLatestPosts()
                R.id.rbPopular1 -> viewModel.loadPostsByTopic(1) // Thay ID chủ đề thích hợp
                R.id.rbPopular2 -> viewModel.loadPostsByTopic(2) // Thay ID chủ đề thích hợp
                R.id.rbPopular3 -> viewModel.loadPostsByTopic(3) // Thay ID chủ đề thích hợp
            }
        }

        // Xử lý sự kiện khi kéo xuống làm mới
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadPosts()
        }
    }

    private fun observeViewModel() {
        // Quan sát danh sách bài post
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
        }

        // Quan sát trạng thái tải
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading && !swipeRefreshLayout.isRefreshing) {
                View.VISIBLE
            } else {
                View.GONE
            }

            if (!isLoading && swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        // Quan sát lỗi
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
}