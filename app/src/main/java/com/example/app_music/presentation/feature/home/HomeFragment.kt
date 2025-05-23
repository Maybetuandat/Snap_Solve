package com.example.app_music.presentation.feature.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.app_music.R
import com.example.app_music.databinding.FragmentHomeBinding
import com.example.app_music.domain.model.SearchHistory
import com.example.app_music.domain.model.User
import com.example.app_music.domain.utils.UrlUtils
import com.example.app_music.presentation.feature.camera.CameraActivity
import com.example.app_music.presentation.feature.camera.ResultActivity
import com.example.app_music.presentation.feature.noteScene.NoteActivity
import com.example.app_music.presentation.feature.searchhistory.SearchHistoryActivity
import com.example.app_music.presentation.feature.textsearch.TextSearchActivity
import com.example.app_music.presentation.feature.translate.TranslateActivity
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupClickListeners()
        setupObservers()

        // Load search history when the fragment is created
        viewModel.loadUserInfo(requireContext())
        viewModel.loadSearchHistory(requireContext())
    }

    private fun setupObservers() {
        viewModel.searchHistory.observe(viewLifecycleOwner) { historyList ->
            updateSearchHistoryUI(historyList)
        }

        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            updateUserUI(user)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSearchHistoryUI(historyList: List<SearchHistory>) {
        // Update search history section title
        binding.missionsSectionTitle.text = getString(R.string.recent_searches)

        // Show/hide search history items based on list size
        if (historyList.isEmpty()) {
            binding.missionItems1.visibility = View.GONE
            binding.missionItems2.visibility = View.GONE
            binding.missionItems3.visibility = View.GONE
            binding.divider1.visibility = View.GONE
            binding.divider2.visibility = View.GONE
            return
        }

        // Create a date formatter for displaying dates
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

        // Update UI for each history item
        historyList.forEachIndexed { index, searchHistory ->
            when (index) {
                0 -> setupHistoryItem(
                    binding.missionItems1,
                    binding.searchHistoryImage1,
                    binding.searchHistoryQuestion1,
                    binding.searchHistoryDate1,
                    searchHistory,
                    dateFormatter
                )
                1 -> {
                    binding.missionItems2.visibility = View.VISIBLE
                    binding.divider1.visibility = View.VISIBLE
                    setupHistoryItem(
                        binding.missionItems2,
                        binding.searchHistoryImage2,
                        binding.searchHistoryQuestion2,
                        binding.searchHistoryDate2,
                        searchHistory,
                        dateFormatter
                    )
                }
                2 -> {
                    binding.missionItems3.visibility = View.VISIBLE
                    binding.divider2.visibility = View.VISIBLE
                    setupHistoryItem(
                        binding.missionItems3,
                        binding.searchHistoryImage3,
                        binding.searchHistoryQuestion3,
                        binding.searchHistoryDate3,
                        searchHistory,
                        dateFormatter
                    )
                }
            }
        }

        // Hide unused items
        if (historyList.size < 2) {
            binding.missionItems2.visibility = View.GONE
            binding.divider1.visibility = View.GONE
        }
        if (historyList.size < 3) {
            binding.missionItems3.visibility = View.GONE
            binding.divider2.visibility = View.GONE
        }

        // Update the "Open" button text
        binding.openMissionsButton.text = getString(R.string.see_more_searches)
    }

    private fun setupHistoryItem(
        container: View,
        imageView: ImageView,
        questionTextView: TextView,
        dateTextView: TextView,
        searchHistory: SearchHistory,
        dateFormatter: DateTimeFormatter
    ) {
        // Load image if available, otherwise show placeholder
        if (!searchHistory.image.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(UrlUtils.getAbsoluteUrl(searchHistory.image))
                .placeholder(R.drawable.placeholder_image)
                .into(imageView)
            Log.d("HistoryUrl:", UrlUtils.getAbsoluteUrl(searchHistory.image))
        } else {
            imageView.setImageResource(R.drawable.ic_search_history)
        }

        // Set question text with ellipsis if too long
        val questionText = searchHistory.question
        questionTextView.text = if (questionText.length > 40) {
            "${questionText.substring(0, 37)}..."
        } else {
            questionText
        }

        // Set date text
        dateTextView.text = searchHistory.createDate

        // Set click listener
        container.setOnClickListener {
            openSearchResult(searchHistory)
        }
    }

    private fun openSearchResult(searchHistory: SearchHistory) {
        val intent = Intent(requireContext(), ResultActivity::class.java).apply {
            // Determine if it's a text or image search
            if (searchHistory.image.isNullOrEmpty()) {
                putExtra("SEARCH_QUERY", searchHistory.question)
                putExtra("IS_TEXT_SEARCH", true)
            } else {
                putExtra("SEARCH_QUERY", searchHistory.question)
                putExtra("IMAGE_URL", UrlUtils.getAbsoluteUrl(searchHistory.image))
                putExtra("FROM_HISTORY", true)
                putExtra("IS_TEXT_SEARCH", false)
            }

            // Add available assignment IDs
            putExtra("ASSIGNMENT_ID_1", searchHistory.assignmentId1)
            putExtra("ASSIGNMENT_ID_2", searchHistory.assignmentId2)
            putExtra("ASSIGNMENT_ID_3", searchHistory.assignmentId3)
            putExtra("ASSIGNMENT_ID_4", searchHistory.assignmentId4)
            putExtra("ASSIGNMENT_ID_5", searchHistory.assignmentId5)
        }
        startActivity(intent)
    }

    private fun setupClickListeners() {
        // Profile and notification
        binding.profileIcon.setOnClickListener {
            showMessage("Profile clicked")
        }

        // Search functionality - Click to open text search
        binding.searchEditText.setOnClickListener {
            startTextSearchActivity()
        }

        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Clear focus and open text search
                binding.searchEditText.clearFocus()
                startTextSearchActivity()
            }
        }

        // Camera button (next to search)
        binding.cameraButton.setOnClickListener {
            startCameraActivity()
        }

        // Utilities section
        val utilitiesLayout = view?.findViewById<ViewGroup>(R.id.utilities_container)
        binding.dictionaryButton.setOnClickListener {
            startTranslateActivity()
        }

        utilitiesLayout?.getChildAt(1)?.findViewById<ViewGroup>(R.id.calculator_item)?.setOnClickListener {
            showMessage("Calculator feature clicked")
        }

        utilitiesLayout?.getChildAt(1)?.findViewById<ViewGroup>(R.id.entertainment_item1)?.setOnClickListener {
            showMessage("Entertainment feature clicked")
        }

        utilitiesLayout?.getChildAt(1)?.findViewById<ViewGroup>(R.id.entertainment_item2)?.setOnClickListener {
            showMessage("More entertainment options clicked")
        }

        // "See more searches" button (previously "Open missions")
        binding.openMissionsButton.setOnClickListener {
            // Navigate to SearchHistoryActivity
            val intent = Intent(requireContext(), SearchHistoryActivity::class.java)
            startActivity(intent)
        }

        // Social buttons
        val facebookUrl = "https://www.facebook.com/" // hoặc profile cá nhân
        val tiktokUrl = "https://www.tiktok.com/" // thay username

        view?.findViewById<View>(R.id.facebook_button)?.setOnClickListener {
            openLink(requireContext(), facebookUrl)
        }

        view?.findViewById<View>(R.id.tiktok_button)?.setOnClickListener {
            openLink(requireContext(), tiktokUrl)
        }


    }

    private fun startTranslateActivity() {
        val intent = Intent(requireContext(), TranslateActivity::class.java)
        startActivity(intent)
    }

    private fun startCameraActivity() {
        val intent = Intent(requireContext(), CameraActivity::class.java)
        startActivity(intent)
    }

    private fun startTextSearchActivity() {
        val intent = Intent(requireContext(), TextSearchActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToNoteActivity() {
        val intent = Intent(requireContext(), NoteActivity::class.java)
        startActivity(intent)
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        // Tải lại dữ liệu mỗi khi Fragment được hiển thị lại
        if (::viewModel.isInitialized) {
            viewModel.loadSearchHistory(requireContext())
        }
    }

    private fun openLink(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Không tìm thấy ứng dụng để mở link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserUI(user: User) {
        // Update avatar
        if (!user.avatarUrl.isNullOrEmpty()) {
            Log.d("AvatarURL", user.avatarUrl ?: "null")
            val avatarUrl = UrlUtils.getAbsoluteUrl(user.avatarUrl)


            Glide.with(requireContext())
                .load(avatarUrl)
                .timeout(15000)
                .placeholder(R.drawable.ic_home_avatar) // Default placeholder
                .error(R.drawable.ic_home_avatar) // Error fallback
                .circleCrop() // Make it circular
                .into(binding.profileIcon)

        } else {
            // Use default avatar if no avatar URL
            binding.profileIcon.setImageResource(R.drawable.ic_home_avatar)
        }
    }
}