package com.example.app_music.presentation.feature.community.communitySearchPost

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.app_music.R
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.presentation.feature.community.adapter.PostAdapter
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetDialog

class SearchResultFragment : Fragment() {
    private lateinit var viewModel: SearchResultViewModel
    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var postAdapter: PostAdapter
    private lateinit var radioGroupSort: RadioGroup
    private lateinit var rbNewest: RadioButton
    private lateinit var rbPopular: RadioButton
    private lateinit var btnFilter: Button
    private lateinit var tvNoResults: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var tvSearchQuery: TextView

    private var currentUserId: Long = 0
    private var searchQuery: String = ""

    companion object {
        private const val ARG_SEARCH_QUERY = "search_query"

        fun newInstance(searchQuery: String): SearchResultFragment {
            val fragment = SearchResultFragment()
            val args = Bundle()
            args.putString(ARG_SEARCH_QUERY, searchQuery)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Đảm bảo chúng ta lấy tham số chính xác
        searchQuery = arguments?.getString("searchQuery") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[SearchResultViewModel::class.java]

        // Lấy ID người dùng hiện tại
        currentUserId = UserPreference.getUserId(requireContext())

        // Khởi tạo các view
        findViews(view)

        // Thiết lập RecyclerView
        setupRecyclerView()

        // Thiết lập sự kiện click cho các bài viết
        setupPostClickListeners()

        // Thiết lập các sự kiện khác
        setupListeners()

        // Quan sát ViewModel
        observeViewModel()

        // Hiển thị query đã tìm kiếm
        tvSearchQuery.text = getString(R.string.post_search_header, searchQuery)

        // Tải kết quả tìm kiếm
        if (searchQuery.isNotEmpty()) {
            // Hiển thị loading trước khi tìm kiếm
            progressBar.visibility = View.VISIBLE
            viewModel.searchPosts(searchQuery)
        } else {
            // Hiển thị thông báo nếu không có từ khóa
            tvNoResults.visibility = View.VISIBLE
            recyclerViewResults.visibility = View.GONE
        }
    }

    private fun findViews(view: View) {
        recyclerViewResults = view.findViewById(R.id.recyclerViewResults)
        progressBar = view.findViewById(R.id.progressBar)
        radioGroupSort = view.findViewById(R.id.radioGroupSort)
        rbNewest = view.findViewById(R.id.rbNewest)
        rbPopular = view.findViewById(R.id.rbPopular)
        btnFilter = view.findViewById(R.id.btnFilter)
        tvNoResults = view.findViewById(R.id.tvNoResults)
        btnBack = view.findViewById(R.id.btnBack)
        tvSearchQuery = view.findViewById(R.id.tvSearchQuery)
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter()
        postAdapter.setCurrentUserId(currentUserId)
        recyclerViewResults.adapter = postAdapter
    }

    private fun setupPostClickListeners() {
        // Xử lý khi người dùng nhấp vào bài đăng
        postAdapter.setOnPostClickListener { post ->
            val bundle = Bundle().apply {
                putLong("postId", post.id)
            }
            findNavController().navigate(R.id.action_searchResultFragment_to_postDetailFragment, bundle)
        }

        // Xử lý khi người dùng nhấn nút like
        postAdapter.setOnLikeClickListener { post ->
            viewModel.toggleLikePost(post, currentUserId)
        }

        // Xử lý khi người dùng nhấn nút comment
        postAdapter.setOnCommentClickListener { post ->
            val bundle = Bundle().apply {
                putLong("postId", post.id)
            }
            findNavController().navigate(R.id.action_searchResultFragment_to_postDetailFragment, bundle)
        }
    }

    private fun setupListeners() {
        // Nút back
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Xử lý sự kiện thay đổi kiểu sắp xếp
        radioGroupSort.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbNewest -> viewModel.changeSortType(SearchResultViewModel.SortType.NEWEST)
                R.id.rbPopular -> viewModel.changeSortType(SearchResultViewModel.SortType.POPULAR)
            }
        }

        // Xử lý sự kiện click vào nút filter
        btnFilter.setOnClickListener {
            showTopicFilterDialog()
        }
    }

    private fun observeViewModel() {
        // Quan sát kết quả tìm kiếm
        viewModel.searchResults.observe(viewLifecycleOwner) { posts ->
            if (posts.isEmpty()) {
                // Hiển thị thông báo không tìm thấy kết quả
                recyclerViewResults.visibility = View.GONE
                tvNoResults.visibility = View.VISIBLE
            } else {
                // Hiển thị danh sách kết quả
                recyclerViewResults.visibility = View.VISIBLE
                tvNoResults.visibility = View.GONE

                // Cập nhật adapter
                postAdapter.submitList(posts)
            }
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

        // Quan sát topic đã chọn
        viewModel.selectedTopicName.observe(viewLifecycleOwner) { topicName ->
            if (topicName.isEmpty()) {
                btnFilter.text = getString(R.string.post_search_filter)
            } else {
                btnFilter.text = getString(R.string.post_search_filter_topic, topicName)
            }
        }
    }

    private fun showTopicFilterDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_filter_topics, null)
        dialog.setContentView(view)

        val flexboxTopics = view.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.flexboxTopics)
        val btnApply = view.findViewById<Button>(R.id.btnApply)
        val btnReset = view.findViewById<Button>(R.id.btnReset)

        var selectedRadioButton: RadioButton? = null
        val currentFilterId = viewModel.currentTopicFilter.value

        // Tạo radio button cho mỗi topic và thêm vào FlexboxLayout
        viewModel.topics.value?.forEach { topic ->
            val radioButton = RadioButton(requireContext()).apply {
                text = topic.name
                id = topic.id.toInt()

                // Ẩn icon radio button mặc định
                buttonDrawable = null

                // Thiết lập style cho radio button
                textSize = 16f
                setPadding(28, 12, 28, 12)
                setBackgroundResource(R.drawable.tab_selector)

                // Sử dụng ColorStateList để tự động thay đổi màu chữ
                setTextColor(resources.getColorStateList(R.color.tab_text_selector, null))

                // Chọn radio button nếu topic này đang được lọc
                if (currentFilterId == topic.id) {
                    isChecked = true
                    selectedRadioButton = this
                }

                // Xử lý click để đảm bảo chỉ một radio button được chọn
                setOnClickListener {
                    // Bỏ check tất cả radio button khác
                    for (i in 0 until flexboxTopics.childCount) {
                        val child = flexboxTopics.getChildAt(i)
                        if (child is RadioButton && child != this) {
                            child.isChecked = false
                        }
                    }

                    // Cập nhật selectedRadioButton
                    selectedRadioButton = if (isChecked) this else null
                }
            }

            // Thiết lập layout params cho flexbox
            val layoutParams = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                com.google.android.flexbox.FlexboxLayout.LayoutParams.WRAP_CONTENT,
                com.google.android.flexbox.FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8)
            }

            // Thêm vào flexbox
            flexboxTopics.addView(radioButton, layoutParams)
        }

        btnApply.setOnClickListener {
            selectedRadioButton?.let { radioButton ->
                viewModel.setTopicFilter(radioButton.id.toLong())
            }
            dialog.dismiss()
        }

        btnReset.setOnClickListener {
            // Bỏ chọn tất cả radio button
            for (i in 0 until flexboxTopics.childCount) {
                val child = flexboxTopics.getChildAt(i)
                if (child is RadioButton) {
                    child.isChecked = false
                }
            }
            selectedRadioButton = null
            viewModel.setTopicFilter(null)
            dialog.dismiss()
        }

        dialog.show()
    }
}