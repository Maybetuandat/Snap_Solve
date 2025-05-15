package com.example.app_music.presentation.feature.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.app_music.R
import com.example.app_music.databinding.FragmentHomeBinding
import com.example.app_music.presentation.noteScene.NoteActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Profile and notification
        binding.profileIcon.setOnClickListener {
            showMessage("Profile clicked")
        }

        binding.notificationIcon.setOnClickListener {
            showMessage("Notifications clicked")
        }

        binding.starsContainer.setOnClickListener {
            showMessage("Stars balance clicked")
        }

        // Search functionality
        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showMessage("Ready to type your question")
            }
        }

        // Camera button (next to search)
        view?.findViewById<View>(R.id.camera_button)?.setOnClickListener {
            showMessage("Camera feature clicked")
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
}