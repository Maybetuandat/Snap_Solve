package com.example.app_music.presentation.feature.community.communityPosting

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.app_music.R
import androidx.constraintlayout.widget.ConstraintLayout

class CommunityPostingFragment : Fragment() {
    private lateinit var btnClose: ImageButton
    private lateinit var btnPost: TextView
    private lateinit var topicSelector: ConstraintLayout
    private lateinit var tvTopicLabel: TextView
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var tvImageCount: TextView
    private lateinit var btnAddImage: ImageButton

    private var selectedTopic: String? = null
    private val maxImageCount = 10 // Maximum number of images allowed
    private var selectedImagesCount = 0 // Current count of selected images

    // This would be a list of image URIs or file paths in a real implementation
    private val selectedImages = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community_posting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        btnClose = view.findViewById(R.id.btnClose)
        btnPost = view.findViewById(R.id.btnPost)
        topicSelector = view.findViewById(R.id.topicSelector)
        tvTopicLabel = view.findViewById(R.id.tvTopicLabel)
        etTitle = view.findViewById(R.id.etTitle)
        etContent = view.findViewById(R.id.etContent)
        tvImageCount = view.findViewById(R.id.tvCharCount) // This is actually our image counter
        btnAddImage = view.findViewById(R.id.btnAddImage)

        // Set up listeners
        setupClickListeners()

        // Initial update of the image counter
        updateImageCounter()
    }

    private fun setupClickListeners() {
        // Close button - navigate back
        btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        // Post button - publish the content
        btnPost.setOnClickListener {
            if (validatePost()) {
                publishPost()
            }
        }

        // Topic selector - show topic selection dialog/fragment
        topicSelector.setOnClickListener {
            showTopicSelector()
        }

        // Add image button
        btnAddImage.setOnClickListener {
            if (selectedImagesCount < maxImageCount) {
                selectImage()
            } else {
                Toast.makeText(requireContext(), "Maximum $maxImageCount images allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateImageCounter() {
        tvImageCount.text = "$selectedImagesCount/$maxImageCount"

        // Optional: Change text color based on how close to the limit
        if (selectedImagesCount >= maxImageCount) {
            tvImageCount.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
            btnAddImage.alpha = 0.5f // Visually indicate button is disabled
        } else {
            tvImageCount.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            btnAddImage.alpha = 1.0f
        }
    }

    private fun validatePost(): Boolean {
        // Check if a topic is selected
        if (selectedTopic == null) {
            Toast.makeText(requireContext(), "Please select a topic", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if title is not empty
        if (etTitle.text.toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            etTitle.requestFocus()
            return false
        }

        // Check if content is not empty
        if (etContent.text.toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please enter content", Toast.LENGTH_SHORT).show()
            etContent.requestFocus()
            return false
        }

        return true
    }

    private fun publishPost() {
        // Here you would typically:
        // 1. Create a post object with the entered data
        // 2. Upload any selected images
        // 3. Send it to your backend/database
        // 4. Show a loading indicator during the process

        // For now, we'll just show a success message and navigate back
        val imageText = if (selectedImagesCount > 0) " with $selectedImagesCount images" else ""
        Toast.makeText(requireContext(), "Post published successfully$imageText", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun showTopicSelector() {
        // Create and show the bottom sheet for topic selection
        val topicBottomSheet = TopicSelectionBottomSheet.newInstance()

        // Set up the listener to receive the selected topic
        topicBottomSheet.setTopicSelectionListener(object :
            TopicSelectionBottomSheet.TopicSelectionListener {
            override fun onTopicSelected(topic: String) {
                selectedTopic = topic
                tvTopicLabel.text = selectedTopic
            }
        })

        // Show the bottom sheet
        topicBottomSheet.show(parentFragmentManager, TopicSelectionBottomSheet.TAG)
    }

    private fun selectImage() {
        // This would typically show the device's image picker
        // For demonstration purposes, we'll simulate adding an image

        // In a real app, you would:
        // 1. Launch the image picker intent
        // 2. Handle the result in onActivityResult
        // 3. Process the selected image(s)
        // 4. Add them to your list

        // Simulating adding an image
        if (selectedImagesCount < maxImageCount) {
            selectedImages.add("dummy_image_uri_${selectedImagesCount + 1}")
            selectedImagesCount++
            updateImageCounter()

            // Show a confirmation toast
            Toast.makeText(requireContext(), "Image $selectedImagesCount added", Toast.LENGTH_SHORT).show()

            // Here you would typically also update the UI to show thumbnails of selected images
        }
    }
}