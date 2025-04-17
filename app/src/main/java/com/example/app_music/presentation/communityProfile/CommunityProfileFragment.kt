package com.example.app_music.presentation.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.app_music.R
import com.google.android.material.tabs.TabLayout

class CommunityProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up back button
        val backButton = view.findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Set up TabLayout
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Handle tab selection - load appropriate posts
                when (tab?.position) {
                    0 -> loadUserPosts() // Your posts
                    1 -> loadLikedPosts() // Liked posts
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Initially load user posts
        loadUserPosts()
    }

    private fun loadUserPosts() {
        // This would fetch and display the user's own posts
        // For now we're just using the sample data in the recycler view
    }

    private fun loadLikedPosts() {
        // This would fetch and display posts the user has liked
        // For now we're just using the sample data in the recycler view
    }
}