package com.example.app_music.presentation.feature.community.communitySearchPost

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.app_music.R

class SearchPostFragment : Fragment() {
    private lateinit var viewModel: SearchPostViewModel
    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var tvDeleteAll: TextView
    private lateinit var recentSearchContainer: ViewGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[SearchPostViewModel::class.java]

        // Khởi tạo các view
        etSearch = view.findViewById(R.id.etSearch)
        btnBack = view.findViewById(R.id.btnBack)
        tvDeleteAll = view.findViewById(R.id.tvDeleteAll)
        recentSearchContainer = view.findViewById(R.id.recentSearchContainer)

        // Tự động focus vào ô tìm kiếm và hiển thị bàn phím
        etSearch.requestFocus()
        etSearch.postDelayed({ showKeyboard() }, 200)

        // Thiết lập các sự kiện khác
        setupListeners()

        // Quan sát ViewModel
        observeViewModel()

        // Tải lịch sử tìm kiếm
        viewModel.loadRecentSearches(requireContext())
    }

    private fun setupListeners() {
        // Xử lý sự kiện khi người dùng nhấn tìm kiếm trên bàn phím
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearch.text.toString())
                return@setOnEditorActionListener true
            }
            false
        }

        // Xử lý sự kiện click vào nút back
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Xử lý sự kiện click vào "Delete all"
        tvDeleteAll.setOnClickListener {
            viewModel.clearRecentSearches(requireContext())
        }
    }

    private fun observeViewModel() {
        // Quan sát lịch sử tìm kiếm
        viewModel.recentSearches.observe(viewLifecycleOwner) { searches ->
            updateRecentSearchesUI(searches)
        }
    }

    private fun updateRecentSearchesUI(searches: List<RecentSearch>) {
        // Xóa tất cả chip hiện tại
        recentSearchContainer.removeAllViews()

        if (searches.isEmpty()) {
            // Ẩn nút Delete all nếu không có lịch sử
            tvDeleteAll.visibility = View.GONE
            return
        }

        // Hiển thị nút Delete all
        tvDeleteAll.visibility = View.VISIBLE

        // Tạo TextView cho mỗi từ khóa trong lịch sử
        for (search in searches) {
            val textView = TextView(requireContext()).apply {
                text = search.query
                setPadding(32, 16, 32, 16)
                setBackgroundResource(R.drawable.search_item_background)
                setTextColor(resources.getColor(R.color.selectedicon, null))

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 16, 16)
                layoutParams = params

                setOnClickListener {
                    etSearch.setText(search.query)
                    performSearch(search.query)
                }
            }
            recentSearchContainer.addView(textView)
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return

        // Ẩn bàn phím
        hideKeyboard()

        // Lưu vào lịch sử tìm kiếm
        viewModel.saveToRecentSearches(query, requireContext())

        // Chuyển đến fragment kết quả tìm kiếm
        val bundle = Bundle().apply {
            putString("searchQuery", query)
        }
        findNavController().navigate(R.id.action_searchPostFragment_to_searchResultFragment, bundle)
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        // Hiển thị bàn phím khi fragment được hiển thị
        etSearch.postDelayed({ showKeyboard() }, 200)
    }

    override fun onPause() {
        super.onPause()
        // Ẩn bàn phím khi rời khỏi fragment
        hideKeyboard()
    }
}