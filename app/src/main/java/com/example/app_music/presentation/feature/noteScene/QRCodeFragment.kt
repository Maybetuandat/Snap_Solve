package com.example.app_music.presentation.feature.noteScene

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.app_music.R
import com.example.app_music.data.model.NotePage
import com.example.app_music.data.repository.FirebaseNoteRepository
import com.example.app_music.databinding.FragmentQrCodeBinding
import com.example.app_music.utils.QRCodeGenerator
import com.example.app_music.utils.StorageManager
import kotlinx.coroutines.launch

class QRCodeFragment : DialogFragment() {

    private var _binding: FragmentQrCodeBinding? = null
    private val binding get() = _binding!!

    private var qrCodeBitmap: Bitmap? = null
    private var shareableUrl: String = ""
    private var isFolder: Boolean = false
    private var noteId: String = ""

    private val repository = FirebaseNoteRepository()
    private lateinit var storageManager: StorageManager

    // List of pages for multi-page notes
    private var pages = mutableListOf<NotePage>()
    private var selectedPageIds = mutableSetOf<String>()

    companion object {
        private const val ARG_ID = "arg_id"
        private const val ARG_IS_FOLDER = "arg_is_folder"

        fun newInstance(id: String, isFolder: Boolean): QRCodeFragment {
            return QRCodeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ID, id)
                    putBoolean(ARG_IS_FOLDER, isFolder)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrCodeBinding.inflate(inflater, container, false)
        dialog?.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        storageManager = StorageManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteId = arguments?.getString(ARG_ID) ?: return
        isFolder = arguments?.getBoolean(ARG_IS_FOLDER, false) ?: false

        setupUI()
        setupListeners()

        // Load pages if sharing a note
        if (!isFolder) {
            loadPages()
        }
    }

    private fun setupUI() {
        val itemType = if (isFolder) "folder" else "note"
        binding.tvTitle.text = "Share this $itemType"
        binding.tvDescription.text = "Scan this QR code to collaborate on this $itemType"

        // Only show page selection for notes
        binding.pagesContainer.visibility = if (isFolder) View.GONE else View.VISIBLE

        // Generate QR code for the whole note/folder initially
        lifecycleScope.launch {
            generateQrCode(noteId)
        }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnShare.setOnClickListener {
            shareUrl()
        }

        binding.selectAllPages.setOnClickListener {
            val checkBox = it as CheckBox
            if (checkBox.isChecked) {
                // Select all pages
                selectedPageIds.clear()
                selectedPageIds.addAll(pages.map { page -> page.id })

                // Update all page checkboxes
                val pagesLayout = binding.pagesLayout
                for (i in 0 until pagesLayout.childCount) {
                    val view = pagesLayout.getChildAt(i)
                    val pageCheckBox = view.findViewById<CheckBox>(R.id.page_checkbox)
                    pageCheckBox?.isChecked = true
                }
            } else {
                // Deselect all pages - but we should always have at least one
                if (selectedPageIds.size > 1) {
                    // Keep only the first page selected
                    val firstPageId = selectedPageIds.first()
                    selectedPageIds.clear()
                    selectedPageIds.add(firstPageId)

                    // Update all page checkboxes
                    val pagesLayout = binding.pagesLayout
                    for (i in 0 until pagesLayout.childCount) {
                        val view = pagesLayout.getChildAt(i)
                        val pageCheckBox = view.findViewById<CheckBox>(R.id.page_checkbox)
                        val pageId = view.tag as? String
                        pageCheckBox?.isChecked = pageId == firstPageId
                    }
                } else {
                    // If we only have one selected, keep it checked
                    checkBox.isChecked = true
                }
            }

            // Update QR code with selected pages
            updateQrCodeForSelectedPages()
        }
    }

    private fun loadPages() {
        lifecycleScope.launch {
            try {
                val pagesResult = repository.getPages(noteId)

                if (pagesResult.isSuccess) {
                    pages = pagesResult.getOrNull()?.toMutableList() ?: mutableListOf()

                    // Sort pages by index
                    pages.sortBy { it.pageIndex }

                    // Initially select all pages
                    selectedPageIds.clear()
                    selectedPageIds.addAll(pages.map { it.id })

                    // Update UI
                    updatePageSelection()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Error loading pages: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePageSelection() {
        val pagesLayout = binding.pagesLayout
        pagesLayout.removeAllViews()

        // Add checkbox for each page
        for ((index, page) in pages.withIndex()) {
            val pageView = layoutInflater.inflate(R.layout.item_share_page, null)
            val pageCheckBox = pageView.findViewById<CheckBox>(R.id.page_checkbox)
            val pageText = pageView.findViewById<TextView>(R.id.page_text)

            pageText.text = "Page ${index + 1}"
            pageCheckBox.isChecked = selectedPageIds.contains(page.id)

            // Set tag to identify the page
            pageView.tag = page.id

            // Set listener
            pageCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedPageIds.add(page.id)
                } else {
                    // Don't allow unchecking if it's the last selected page
                    if (selectedPageIds.size > 1) {
                        selectedPageIds.remove(page.id)
                    } else {
                        pageCheckBox.isChecked = true
                        Toast.makeText(requireContext(),
                            "At least one page must be selected", Toast.LENGTH_SHORT).show()
                    }
                }

                // Update "Select All" checkbox
                binding.selectAllPages.isChecked = selectedPageIds.size == pages.size

                // Update QR code
                updateQrCodeForSelectedPages()
            }

            pagesLayout.addView(pageView)
        }

        // Update "Select All" checkbox
        binding.selectAllPages.isChecked = selectedPageIds.size == pages.size
    }

    private fun updateQrCodeForSelectedPages() {
        lifecycleScope.launch {
            if (selectedPageIds.isEmpty()) {
                // If somehow no pages are selected, generate QR code for the whole note
                generateQrCode(noteId)
            } else if (selectedPageIds.size == pages.size) {
                // If all pages are selected, generate QR code for the whole note
                generateQrCode(noteId)
            } else {
                // Generate QR code for selected pages
                generateQrCodeForPages(selectedPageIds.toList())
            }
        }
    }

    private suspend fun generateQrCode(id: String) {
        shareableUrl = if (isFolder) {
            "snapsolve://folders/$id"
        } else {
            "snapsolve://notes/$id"
        }

        // Generate QR code
        qrCodeBitmap = QRCodeGenerator.generateQRCode(shareableUrl)
        qrCodeBitmap?.let {
            binding.ivQrCode.setImageBitmap(it)
        }
    }

    private suspend fun generateQrCodeForPages(pageIds: List<String>) {
        // Create a special URL that includes specific pages
        shareableUrl = "snapsolve://notes/$noteId?pages=${pageIds.joinToString(",")}"

        // Generate QR code
        qrCodeBitmap = QRCodeGenerator.generateQRCode(shareableUrl)
        qrCodeBitmap?.let {
            binding.ivQrCode.setImageBitmap(it)
        }
    }

    private fun shareUrl() {
        // First copy to clipboard
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Shareable Link", shareableUrl)
        clipboard.setPrimaryClip(clip)

        // Then share via intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Join me to collaborate on this ${if (isFolder) "folder" else "note"}: $shareableUrl")
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}