package com.example.app_music.presentation.community

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.example.app_music.R
import com.example.app_music.presentation.searchPost.SearchPostActivity

class CommunityFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tìm nút tìm kiếm
        val searchButton = view.findViewById<ImageButton>(R.id.btnSearch)

        // Thiết lập sự kiện click cho nút tìm kiếm
        searchButton.setOnClickListener {
            // Mở SearchActivity
            val intent = Intent(activity, SearchPostActivity::class.java)
            startActivity(intent)

            // Thêm hiệu ứng chuyển màn hình
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}