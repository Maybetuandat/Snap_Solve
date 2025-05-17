package com.example.app_music.presentation.feature.qrscanner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app_music.databinding.ActivityQrScannerBinding
import com.example.app_music.presentation.feature.noteScene.NoteActivity
import com.example.app_music.presentation.feature.noteScene.NoteDetailActivity
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import java.util.regex.Pattern

class QRScannerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var captureManager: CaptureManager
    private var isProcessingResult = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupScanner(savedInstanceState)
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupScanner(savedInstanceState: Bundle?) {
        val barcodeView = binding.barcodeScanner
        captureManager = CaptureManager(this, barcodeView)
        captureManager.initializeFromIntent(intent, savedInstanceState)
        
        // Setup callback for scan results
        barcodeView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (isProcessingResult) return
                isProcessingResult = true
                
                val scanResult = result.text
                processScanResult(scanResult)
            }
            
            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {
                // Not used
            }
        })
    }
    
    private fun processScanResult(scanResult: String) {
        binding.progressBar.visibility = View.VISIBLE
        
        // Check if the scan result is a valid note or folder URL
        val notePattern = Pattern.compile("snapsolve://notes/([a-zA-Z0-9-]+)")
        val folderPattern = Pattern.compile("snapsolve://folders/([a-zA-Z0-9-]+)")
        
        val noteMatcher = notePattern.matcher(scanResult)
        val folderMatcher = folderPattern.matcher(scanResult)
        
        when {
            noteMatcher.matches() -> {
                val noteId = noteMatcher.group(1)
                openNoteDetail(noteId)
            }
            folderMatcher.matches() -> {
                val folderId = folderMatcher.group(1)
                openFolder(folderId)
            }
            else -> {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show()
                isProcessingResult = false
                binding.barcodeScanner.decodeSingle(null) // Reset scanner
            }
        }
    }
    
    private fun openNoteDetail(noteId: String) {
        // Open the NoteDetailActivity with the scanned note ID
        val intent = Intent(this, NoteDetailActivity::class.java).apply {
            putExtra("note_id", noteId)
            putExtra("from_qr_code", true)
        }
        startActivity(intent)
        finish()
    }
    
    private fun openFolder(folderId: String) {
        // Open the NoteActivity with the scanned folder ID
        val intent = Intent(this, NoteActivity::class.java).apply {
            putExtra("folder_id", folderId)
            putExtra("from_qr_code", true)
        }
        startActivity(intent)
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        captureManager.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        captureManager.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        captureManager.onDestroy()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        captureManager.onSaveInstanceState(outState)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        captureManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}