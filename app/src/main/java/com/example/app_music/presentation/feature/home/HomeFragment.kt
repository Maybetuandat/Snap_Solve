package com.example.app_music.presentation.feature.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.app_music.R
import com.example.app_music.databinding.FragmentHomeBinding
import com.example.app_music.presentation.feature.camera.CameraActivity
import com.example.app_music.presentation.feature.noteScene.NoteActivity
import com.example.app_music.presentation.feature.notification.NotificationViewModel
import com.example.app_music.presentation.feature.textsearch.TextSearchActivity
import com.example.app_music.utils.attachBadge

import com.google.android.material.badge.BadgeDrawable


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Share ViewModel with Activity
    private val notificationViewModel: NotificationViewModel by activityViewModels()

    // Notification badge for the notification icon
    private var notificationBadge: BadgeDrawable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupNotificationBadge()
        observeNotificationCount()
    }

    private fun setupNotificationBadge() {
        notificationBadge = BadgeDrawable.create(requireContext()).apply {
            backgroundColor = requireContext().getColor(R.color.red)
            badgeTextColor = requireContext().getColor(R.color.white)
            maxCharacterCount = 3
            isVisible = false
        }

        // Attach badge to notification icon when the view is laid out
        binding.notificationIcon.post {
            // Position at top right of the notification icon
            notificationBadge?.let { badge ->
                badge.horizontalOffset = binding.notificationIcon.width / 4
                badge.verticalOffset = -binding.notificationIcon.height / 4
                binding.notificationIcon.attachBadge(badge)
            }
        }
    }

    private fun observeNotificationCount() {
        notificationViewModel.unreadNotificationCount.observe(viewLifecycleOwner) { count ->
            updateNotificationBadge(count)
        }
    }

    private fun updateNotificationBadge(count: Long) {
        notificationBadge?.apply {
            isVisible = count > 0
            number = count.toInt()
            invalidateSelf() // Force redraw
        }
    }

    private fun setupClickListeners() {
        // Profile and notification
        binding.profileIcon.setOnClickListener {
            showMessage("Profile clicked")
        }

        binding.notificationIcon.setOnClickListener {
            showMessage("Notifications clicked")
            // TODO: Navigate to notification screen and mark notifications as read
        }

        binding.starsContainer.setOnClickListener {
            showMessage("Stars balance clicked")
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

        // Camera button (next to search) - UPDATED to start CameraActivity
        binding.cameraButton.setOnClickListener {
            startCameraActivity()
        }

        // Utilities section - Find the proper views using parent layout
        val utilitiesLayout = view?.findViewById<ViewGroup>(R.id.utilities_container)
        utilitiesLayout?.getChildAt(1)?.findViewById<ViewGroup>(R.id.dictionary_item)?.setOnClickListener {
            showMessage("Dictionary feature clicked")
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

        // Mission items
        binding.missionItems1.setOnClickListener {
            showMessage("First mission clicked")
        }

        binding.missionItems2.setOnClickListener {
            showMessage("Second mission clicked")
        }

        binding.missionItems3.setOnClickListener {
            showMessage("Third mission clicked")
        }

        binding.openMissionsButton.setOnClickListener {
            showMessage("Missions board opened")
        }

        // Social buttons
        view?.findViewById<View>(R.id.facebook_button)?.setOnClickListener {
            showMessage("Share on Facebook clicked")
        }

        view?.findViewById<View>(R.id.tiktok_button)?.setOnClickListener {
            showMessage("Share on TikTok clicked")
        }

        // Long press on dictionary to access notes (hidden feature)
        utilitiesLayout?.getChildAt(1)?.findViewById<ViewGroup>(R.id.dictionary_item)?.setOnLongClickListener {
            navigateToNoteActivity()
            true
        }
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

    override fun onResume() {
        super.onResume()
        // Refresh notification count when fragment resumes
        notificationViewModel.refreshNotifications()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}