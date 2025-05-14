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

    private lateinit var recyclerViewYourPosts: androidx.recyclerview.widget.RecyclerView
    private lateinit var recyclerViewLikedPosts: androidx.recyclerview.widget.RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerViews
        recyclerViewYourPosts = view.findViewById(R.id.recyclerViewYourPosts)
        recyclerViewLikedPosts = view.findViewById(R.id.recyclerViewLikedPosts)

        // Set up back button
        val backButton = view.findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Set up TabLayout
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Handle tab selection - show appropriate RecyclerView
                when (tab?.position) {
                    0 -> {
                        recyclerViewYourPosts.visibility = View.VISIBLE
                        recyclerViewLikedPosts.visibility = View.GONE
                        loadUserPosts()
                    }
                    1 -> {
                        recyclerViewYourPosts.visibility = View.GONE
                        recyclerViewLikedPosts.visibility = View.VISIBLE
                        loadLikedPosts()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Set up adapters for both RecyclerViews
        setupRecyclerViews()

        // Initially load user posts
        loadUserPosts()
    }

    private fun setupRecyclerViews() {
        // TODO: Create and set your post adapters here
        // For example:
        // val yourPostsAdapter = PostAdapter()
        // recyclerViewYourPosts.adapter = yourPostsAdapter
        //
        // val likedPostsAdapter = PostAdapter()
        // recyclerViewLikedPosts.adapter = likedPostsAdapter
    }

    private fun loadUserPosts() {
        // In a real app, you would fetch user posts from a data source
        // For example:
        // viewModel.getUserPosts().observe(viewLifecycleOwner) { posts ->
        //     (recyclerViewYourPosts.adapter as PostAdapter).submitList(posts)
        // }
    }

    private fun loadLikedPosts() {
        // In a real app, you would fetch liked posts from a data source
        // For example:
        // viewModel.getLikedPosts().observe(viewLifecycleOwner) { posts ->
        //     (recyclerViewLikedPosts.adapter as PostAdapter).submitList(posts)
        // }
    }
}