package com.example.app_music.presentation.searchPost

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.app_music.R


class SearchPostActivity : AppCompatActivity() {
    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var tvDeleteAll: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_post)

        // Khởi tạo các view
        etSearch = findViewById(R.id.etSearch)
        btnBack = findViewById(R.id.btnBack)
        tvDeleteAll = findViewById(R.id.tvDeleteAll)

        // Tự động focus vào EditText và hiển thị bàn phím
        etSearch.requestFocus()

        etSearch.setOnClickListener {
            // Đảm bảo EditText có focus
            etSearch.requestFocus()

            // Hiển thị bàn phím
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        // Sử dụng postDelayed để đảm bảo view đã được vẽ trước khi hiển thị bàn phím
        etSearch.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
        }, 200)

        // Xử lý sự kiện click vào nút back
        btnBack.setOnClickListener {
            finish() // Sẽ tự động áp dụng animation đã định nghĩa trong phương thức finish()
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
        setupSearchTags()
    }

    override fun finish() {
        super.finish()
        // Thêm hiệu ứng chuyển màn hình khi đóng activity
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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

    private fun setupSearchTags() {
        // Xử lý sự kiện click cho các hashtag và từ khóa tìm kiếm gần đây
        val recentSearch1 = findViewById<TextView>(R.id.tvRecentSearch1)
        val recentSearch2 = findViewById<TextView>(R.id.tvRecentSearch2)
        val hashtag1 = findViewById<TextView>(R.id.tvHashtag1)
        val hashtag2 = findViewById<TextView>(R.id.tvHashtag2)
        val hashtag3 = findViewById<TextView>(R.id.tvHashtag3)

        val clickListener = View.OnClickListener { view ->
            if (view is TextView) {
                etSearch.setText(view.text)
                etSearch.setSelection(etSearch.text.length) // Di chuyển con trỏ đến cuối text
                performSearch(view.text.toString())
            }
        }

        recentSearch1.setOnClickListener(clickListener)
        recentSearch2.setOnClickListener(clickListener)
        hashtag1.setOnClickListener(clickListener)
        hashtag2.setOnClickListener(clickListener)
        hashtag3.setOnClickListener(clickListener)
    }
}