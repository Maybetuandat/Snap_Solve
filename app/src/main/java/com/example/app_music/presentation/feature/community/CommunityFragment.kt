package com.example.app_music.presentation.feature.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.app_music.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

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
            // Chuyển đến SearchPostFragment
            findNavController().navigate(R.id.action_communityFragment_to_searchPostFragment)
        }

        // Tìm nút profile
        val profileButton = view.findViewById<ImageButton>(R.id.btnProfile)

        // Thiết lập sự kiện click cho nút profile
        profileButton.setOnClickListener {
            // Chuyển đến CommunityProfileFragment
            findNavController().navigate(R.id.action_communityFragment_to_communityProfileFragment)
        }

        // Set up create post floating action button
        val fabCreate = view.findViewById<FloatingActionButton>(R.id.fabCreate)
        fabCreate.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_communityPostingFragment)
        }
    }
}