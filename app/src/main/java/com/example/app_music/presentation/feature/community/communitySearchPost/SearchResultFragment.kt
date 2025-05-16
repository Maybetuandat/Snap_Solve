package com.example.app_music.presentation.feature.community.communitySearchPost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.app_music.R
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.presentation.feature.community.adapter.PostAdapter
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
        tvSearchQuery.text = "Kết quả cho: $searchQuery"

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
            // TODO: Triển khai chức năng thích/bỏ thích bài viết từ kết quả tìm kiếm
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
                btnFilter.text = "Filter"
            } else {
                btnFilter.text = "Filter: $topicName"
            }
        }
    }

    private fun showTopicFilterDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_filter_topics, null)
        dialog.setContentView(view)

        val radioGroupTopics = view.findViewById<RadioGroup>(R.id.radioGroupTopics)
        val btnApply = view.findViewById<Button>(R.id.btnApply)
        val btnReset = view.findViewById<Button>(R.id.btnReset)

        // Tạo radio button cho mỗi topic
        viewModel.topics.value?.forEach { topic ->
            val radioButton = RadioButton(requireContext()).apply {
                text = topic.name
                id = topic.id.toInt()

                // Chọn radio button nếu topic này đang được lọc
                if (viewModel.currentTopicFilter.value == topic.id) {
                    isChecked = true
                }
            }
            radioGroupTopics.addView(radioButton)
        }

        btnApply.setOnClickListener {
            val checkedRadioButtonId = radioGroupTopics.checkedRadioButtonId
            if (checkedRadioButtonId != -1) {
                viewModel.setTopicFilter(checkedRadioButtonId.toLong())
            }
            dialog.dismiss()
        }

        btnReset.setOnClickListener {
            radioGroupTopics.clearCheck()
            viewModel.setTopicFilter(null)
            dialog.dismiss()
        }

        dialog.show()
    }
}