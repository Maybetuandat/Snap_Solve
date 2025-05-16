package com.example.app_music.presentation.feature.community.communitySearchPost

import java.io.Serializable

data class RecentSearch(
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable