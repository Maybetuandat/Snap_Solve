package com.example.app_music.presentation.feature.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.app_music.R
import com.example.app_music.data.model.Topic
import com.example.app_music.presentation.feature.community.adapter.PostAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CommunityFragment : Fragment() {

    private lateinit var viewModel: CommunityViewModel
    private lateinit var postAdapter: PostAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var radioGroup: RadioGroup
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // Map to store topic IDs by radio button IDs
    private val topicIdMap = mutableMapOf<Int, Long>()

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
        progressBar = view.findViewById(R.id.progressBar)
        radioGroup = view.findViewById(R.id.radioGroup)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh)

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

        // Xử lý sự kiện khi chọn tab - sẽ được cập nhật sau khi có topics
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbAll) {
                viewModel.loadLatestPosts()
            } else {
                // Lấy topic ID từ map và tải bài viết theo topic đó
                topicIdMap[checkedId]?.let { topicId ->
                    viewModel.loadPostsByTopic(topicId)
                }
            }
        }

        // Xử lý sự kiện khi kéo xuống làm mới
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadPosts()
        }
    }

    private fun observeViewModel() {
        // Quan sát danh sách chủ đề
        viewModel.topics.observe(viewLifecycleOwner) { topics ->
            if (topics.isNotEmpty()) {
                setupTopicRadioButtons(topics)
            }
        }

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

    private fun setupTopicRadioButtons(topics: List<Topic>) {
        // Clear existing radio buttons except "All"
        val allButton = radioGroup.findViewById<RadioButton>(R.id.rbAll)
        radioGroup.removeAllViews()

        // Re-add the "All" button first
        radioGroup.addView(allButton)

        // Create and add radio buttons for each topic
        topics.forEachIndexed { index, topic ->
            val radioButton = RadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = topic.name
                layoutParams = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.WRAP_CONTENT,
                    resources.getDimensionPixelSize(R.dimen.radio_button_height)
                )
                setBackgroundResource(R.drawable.tab_selector)
                buttonDrawable = null // Remove the default radio button
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.radio_button_padding_horizontal),
                    0,
                    resources.getDimensionPixelSize(R.dimen.radio_button_padding_horizontal),
                    0
                )
                setTextColor(resources.getColorStateList(R.color.tab_text_selector, null))
                gravity = android.view.Gravity.CENTER

                // Set margin between radio buttons
                (layoutParams as RadioGroup.LayoutParams).marginEnd =
                    resources.getDimensionPixelSize(R.dimen.radio_button_margin)
            }

            // Add to radio group
            radioGroup.addView(radioButton)

            // Store topic ID for this radio button
            topicIdMap[radioButton.id] = topic.id
        }
    }
}