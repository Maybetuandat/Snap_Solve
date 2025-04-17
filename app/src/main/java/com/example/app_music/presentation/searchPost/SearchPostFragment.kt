package com.example.app_music.presentation.searchPost

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.app_music.R

class SearchPostFragment : Fragment() {
    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var tvDeleteAll: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo các view
        etSearch = view.findViewById(R.id.etSearch)
        btnBack = view.findViewById(R.id.btnBack)
        tvDeleteAll = view.findViewById(R.id.tvDeleteAll)

        // Tự động focus vào EditText và hiển thị bàn phím
        etSearch.requestFocus()

        etSearch.setOnClickListener {
            // Đảm bảo EditText có focus
            etSearch.requestFocus()

            // Hiển thị bàn phím
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        // Sử dụng postDelayed để đảm bảo view đã được vẽ trước khi hiển thị bàn phím
        etSearch.postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
        }, 200)

        // Xử lý sự kiện click vào nút back
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Xử lý sự kiện khi người dùng nhấn Search trên bàn phím
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Thực hiện tìm kiếm ở đây
                performSearch(etSearch.text.toString())
                return@setOnEditorActionListener true
            }
            false
        }

        // Xử lý sự kiện click vào "Delete all"
        tvDeleteAll.setOnClickListener {
            // Xóa tất cả lịch sử tìm kiếm
            clearRecentSearches()
        }

        // Xử lý sự kiện click vào các hashtag hoặc từ khóa tìm kiếm gần đây
        setupSearchTags(view)
    }

    private fun performSearch(query: String) {
        // Xử lý tìm kiếm với từ khóa query
        // Ví dụ: Gọi API tìm kiếm, hiển thị kết quả, v.v.
        // ...

        // Lưu từ khóa vào lịch sử tìm kiếm
        saveToRecentSearches(query)
    }

    private fun saveToRecentSearches(query: String) {
        // Lưu từ khóa vào lịch sử tìm kiếm
        // Có thể sử dụng SharedPreferences hoặc Database
        // ...
    }

    private fun clearRecentSearches() {
        // Xóa tất cả lịch sử tìm kiếm
        // Cập nhật giao diện
        // ...
    }

    private fun setupSearchTags(view: View) {
        // Xử lý sự kiện click cho các hashtag và từ khóa tìm kiếm gần đây
        val recentSearch1 = view.findViewById<TextView>(R.id.tvRecentSearch1)
        val recentSearch2 = view.findViewById<TextView>(R.id.tvRecentSearch2)
        val hashtag1 = view.findViewById<TextView>(R.id.tvHashtag1)
        val hashtag2 = view.findViewById<TextView>(R.id.tvHashtag2)
        val hashtag3 = view.findViewById<TextView>(R.id.tvHashtag3)

        val clickListener = View.OnClickListener { v ->
            if (v is TextView) {
                etSearch.setText(v.text)
                etSearch.setSelection(etSearch.text.length) // Di chuyển con trỏ đến cuối text
                performSearch(v.text.toString())
            }
        }

        recentSearch1.setOnClickListener(clickListener)
        recentSearch2.setOnClickListener(clickListener)
        hashtag1.setOnClickListener(clickListener)
        hashtag2.setOnClickListener(clickListener)
        hashtag3.setOnClickListener(clickListener)
    }
}