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
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
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

    private var pages = mutableListOf<NotePage>()

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

        // Load pages nếu là note
        if (!isFolder) {
            loadPages()
        }
    }

    private fun setupUI() {
        val itemType = if (isFolder) "folder" else "note"
        binding.tvTitle.text = "Share this $itemType"
        binding.tvDescription.text = "Scan this QR code to collaborate on this $itemType"

        // Khởi tạo QR code
        lifecycleScope.launch {
            generateQrCodeForFullItem(noteId)
        }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnShare.setOnClickListener {
            shareUrl()
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
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun generateQrCodeForFullItem(id: String) {
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

    private fun shareUrl() {
        // Copy to clipboard
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Shareable Link", shareableUrl)
        clipboard.setPrimaryClip(clip)

        // Share via intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT,
                "Join me to collaborate on this ${if (isFolder) "folder" else "note"}: $shareableUrl")
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clean up bitmap
        qrCodeBitmap?.recycle()
        qrCodeBitmap = null

        _binding = null
    }
}