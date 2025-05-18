package com.example.app_music.presentation.feature.noteScene

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.app_music.R
import com.example.app_music.data.collaboration.CollaborationManager
import com.example.app_music.data.repository.FirebaseNoteRepository
import com.example.app_music.databinding.ActivityNoteDetailBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.noteScene.views.DrawingView
import com.example.app_music.presentation.noteScene.ColorPickerDialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.util.Random

class NoteDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var repository: FirebaseNoteRepository
    private lateinit var collaborationManager: CollaborationManager

    private var noteId: String = ""
    private var noteTitle: String = ""
    private var fromQrCode: Boolean = false

    // Drawing variables
    private var currentColor = Color.BLACK
    private var currentWidth = 5f

    // User colors for collaboration
    private val userColors = arrayOf(
        Color.RED,
        Color.BLUE,
        Color.GREEN,
        Color.MAGENTA,
        Color.CYAN,
        Color.DKGRAY
    )
    private val userColor = userColors[Random().nextInt(userColors.size)]

    // Firebase Auth
    private val auth = FirebaseAuth.getInstance()
    private val currentUser get() = auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get note details from intent
        noteId = intent.getStringExtra("note_id") ?: ""
        noteTitle = intent.getStringExtra("note_title") ?: ""
        fromQrCode = intent.getBooleanExtra("from_qr_code", false)

        if (noteId.isEmpty()) {
            Toast.makeText(this, "Note ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Firebase repository
        repository = FirebaseNoteRepository()

        // Initialize collaboration manager
        collaborationManager = CollaborationManager(noteId)

        // Set up UI
        setupToolbar()
        setupDrawingTools()
        setupHelpButtons()

        // Load note data
        loadNote()

        // Set up collaboration
        setupCollaboration()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = noteTitle
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupDrawingTools() {
        // Default to PAN mode
        selectMode(DrawingView.DrawMode.PAN)

        // Tool selection listeners
        binding.buttonHand.setOnClickListener {
            selectMode(DrawingView.DrawMode.PAN)
        }

        binding.buttonPen.setOnClickListener {
            selectMode(DrawingView.DrawMode.DRAW)
        }

        binding.buttonEraser.setOnClickListener {
            selectMode(DrawingView.DrawMode.ERASE)
        }

        binding.buttonColor.setOnClickListener {
            showColorPicker()
        }

        binding.buttonStrokeWidth.setOnClickListener {
            showStrokeWidthDialog()
        }

        binding.buttonUndo.setOnClickListener {
            binding.drawingView.undo()
            updateUndoRedoButtons()
            saveDrawing(false)
        }

        binding.buttonRedo.setOnClickListener {
            binding.drawingView.redo()
            updateUndoRedoButtons()
            saveDrawing(false)
        }

        // Set up drawing view listener for auto-saving
        binding.drawingView.setOnDrawCompletedListener(object : DrawingView.OnDrawCompletedListener {
            override fun onDrawCompleted() {
                updateUndoRedoButtons()
                saveDrawing(false)

                // Sync drawing action with collaborators
                syncDrawingAction()
            }
        })
    }

    private fun setupHelpButtons() {
        binding.buttonHelpAi.setOnClickListener {
            Toast.makeText(this, "AI help feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.buttonViewExplanation.setOnClickListener {
            Toast.makeText(this, "Explanation feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    private var previousUserCount = 0
    private fun setupCollaboration() {
        // Mark user as active
        val username = currentUser?.displayName ?: "Anonymous"
        collaborationManager.setUserPresence(username, userColor)

        // Observe active users with improved UI feedback
        lifecycleScope.launch {
            collaborationManager.getActiveUsers().collectLatest { users ->
                binding.activeUsersView.updateActiveUsers(users)
                binding.userCount.text = "${users.size}"

                // Show toast when a new user joins (except for the first user, which is the current user)
                if (previousUserCount > 0 && users.size > previousUserCount) {
                    val newUser = users.lastOrNull { it.userId != currentUser?.uid }
                    if (newUser != null) {
                        Toast.makeText(this@NoteDetailActivity,
                            "${newUser.username} joined", Toast.LENGTH_SHORT).show()
                    }
                }
                previousUserCount = users.size
            }
        }

        // Rest of the collaboration setup
    }

    private fun syncDrawingAction() {
        // Get the current drawing path and convert it to a DrawingAction
        // In a real app, you would implement logic to convert the path to a serializable format
        // This is a simplified placeholder implementation
        val path = binding.drawingView.getCurrentPath()
        if (path != null) {
            val pathPoints = collaborationManager.pathToPointsList(path)
            val drawingAction = CollaborationManager.DrawingAction(
                type = CollaborationManager.ActionType.DRAW,
                path = pathPoints,
                color = currentColor,
                strokeWidth = currentWidth
            )
            collaborationManager.saveDrawingAction(drawingAction)
        }
    }

    private fun loadNote() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Fetch note data in background thread
                val noteResult = withContext(Dispatchers.IO) {
                    repository.getNote(noteId)
                }

                if (noteResult.isSuccess) {
                    val note = noteResult.getOrNull()!!

                    // Update title if needed (UI update on main thread)
                    if (noteTitle.isEmpty()) {
                        noteTitle = note.title
                        supportActionBar?.title = noteTitle
                    }

                    // Load image if available
                    note.imagePath?.let { path ->
                        try {
                            // Use retry pattern for loading images
                            var retryCount = 0
                            var imageLoaded = false
                            var bitmap: Bitmap? = null

                            while (retryCount < 3 && !imageLoaded) {
                                try {
                                    val imageUriResult = withContext(Dispatchers.IO) {
                                        repository.getImageBitmap(path)
                                    }

                                    if (imageUriResult.isSuccess) {
                                        val uri = imageUriResult.getOrNull()

                                        // Use Glide on the main thread
                                        Glide.with(this@NoteDetailActivity)
                                            .load(uri)
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .timeout(20000) // 20 second timeout
                                            .into(binding.imageNote)

                                        imageLoaded = true
                                    } else {
                                        retryCount++
                                        if (retryCount < 3) {
                                            delay(1000) // Wait a second before retrying
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("NoteDetailActivity", "Error loading image (attempt $retryCount): ${e.message}")
                                    retryCount++
                                    if (retryCount < 3) {
                                        delay(1000)
                                    }
                                }
                            }

                            if (!imageLoaded) {
                                binding.imageNote.setImageResource(R.drawable.ic_note)
                                Log.w("NoteDetailActivity", "Could not load image after 3 attempts")
                            }
                        } catch (e: Exception) {
                            Log.e("NoteDetailActivity", "Error loading image: ${e.message}")
                            binding.imageNote.setImageResource(R.drawable.ic_note)
                        }
                    }

                    // Load drawing data if available
                    note.drawingData?.let { drawingData ->
                        try {
                            Log.d("NoteDetailActivity", "Found drawing data, length: ${drawingData.length}")

                            // Convert base64 string back to bitmap
                            val decodedBytes = withContext(Dispatchers.Default) {
                                android.util.Base64.decode(drawingData, android.util.Base64.DEFAULT)
                            }

                            // Create bitmap from bytes
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                            if (bitmap != null) {
                                // We have a valid bitmap to display in the drawing view
                                binding.drawingView.setBackgroundImage(bitmap)
                                Log.d("NoteDetailActivity", "Successfully loaded drawing: ${bitmap.width}x${bitmap.height}")
                            } else {
                                Log.e("NoteDetailActivity", "Failed to decode drawing data")
                            }
                        } catch (e: Exception) {
                            Log.e("NoteDetailActivity", "Error processing drawing data: ${e.message}")
                        }
                    }
                } else {
                    // Handle note fetch failure
                    Toast.makeText(this@NoteDetailActivity,
                        "Could not load note: ${noteResult.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT).show()
                    Log.e("NoteDetailActivity", "Failed to fetch note: ${noteResult.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                // Handle any other exceptions
                Toast.makeText(this@NoteDetailActivity,
                    "Error loading note: ${e.message}",
                    Toast.LENGTH_SHORT).show()
                Log.e("NoteDetailActivity", "Error in loadNote", e)
            } finally {
                // Always hide progress indicator on main thread
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun saveDrawing(showToast: Boolean = true) {
        // Using SupervisorJob() to prevent cancellation from propagating to children
        val job = SupervisorJob()

        lifecycleScope.launch(job + Dispatchers.Main) {
            try {
                binding.saveProgressBar.visibility = View.VISIBLE

                // Use withContext for IO operations but keep the job context
                val result = withContext(Dispatchers.IO) {
                    try {
                        // Get the note first
                        val noteResult = repository.getNote(noteId)

                        if (noteResult.isSuccess) {
                            val note = noteResult.getOrNull()!!

                            // Get combined bitmap (background + drawing)
                            val combinedBitmap = binding.drawingView.getCombinedBitmap()

                            // Convert bitmap to base64 string
                            val baos = ByteArrayOutputStream()
                            combinedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                            val imageBytes = baos.toByteArray()
                            val drawingData = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT)

                            Log.d("NoteDetailActivity", "Drawing data length: ${drawingData.length}")

                            // Only update the drawingData field, preserve other fields
                            val updatedNote = note.copy(
                                drawingData = drawingData,
                                updatedAt = System.currentTimeMillis()
                            )

                            repository.updateNote(updatedNote)
                        } else {
                            Result.failure(Exception("Failed to get note"))
                        }
                    } catch (e: Exception) {
                        Log.e("NoteDetailActivity", "Error saving drawing", e)
                        Result.failure(e)
                    }
                }

                // Now back on the main thread
                binding.saveProgressBar.visibility = View.GONE

                if (result.isSuccess) {
                    if (showToast) {
                        Toast.makeText(this@NoteDetailActivity, "Drawing saved", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("NoteDetailActivity", "Drawing saved successfully")
                } else {
                    if (showToast) {
                        Toast.makeText(this@NoteDetailActivity, "Failed to save drawing: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("NoteDetailActivity", "Failed to save drawing: ${result.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                // Handle exceptions from the main thread
                binding.saveProgressBar.visibility = View.GONE
                if (showToast) {
                    Toast.makeText(this@NoteDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("NoteDetailActivity", "Error in saveDrawing", e)
            }
        }
    }

    private fun selectMode(mode: DrawingView.DrawMode) {
        // Set the drawing mode
        binding.drawingView.setMode(mode)

        // Update UI to highlight selected tool
        binding.buttonHand.setBackgroundResource(
            if (mode == DrawingView.DrawMode.PAN) R.drawable.tool_selected_bg
            else android.R.color.transparent
        )
        binding.buttonPen.setBackgroundResource(
            if (mode == DrawingView.DrawMode.DRAW) R.drawable.tool_selected_bg
            else android.R.color.transparent
        )
        binding.buttonEraser.setBackgroundResource(
            if (mode == DrawingView.DrawMode.ERASE) R.drawable.tool_selected_bg
            else android.R.color.transparent
        )

        // Configure drawing view based on mode
        when (mode) {
            DrawingView.DrawMode.DRAW -> {
                binding.drawingView.setColor(currentColor)
                binding.drawingView.setStrokeWidth(currentWidth)
            }
            DrawingView.DrawMode.ERASE -> {
                binding.drawingView.setStrokeWidth(currentWidth * 2) // Eraser is thicker
            }
            DrawingView.DrawMode.PAN -> {
                // No special configuration needed
            }
        }
    }

    private fun showColorPicker() {
        val colorPickerDialog = ColorPickerDialog(this)
        colorPickerDialog.setOnColorSelectedListener { color ->
            currentColor = color
            binding.buttonColor.setColorFilter(color)

            if (binding.drawingView.getMode() == DrawingView.DrawMode.DRAW) {
                binding.drawingView.setColor(color)
            }
        }
        colorPickerDialog.show()
    }

    private fun showStrokeWidthDialog() {
        val dialog = StrokeWidthDialog(this, currentWidth)
        dialog.setOnStrokeWidthSelectedListener { width ->
            currentWidth = width

            // Update stroke width based on current mode
            if (binding.drawingView.getMode() == DrawingView.DrawMode.DRAW) {
                binding.drawingView.setStrokeWidth(width)
            } else if (binding.drawingView.getMode() == DrawingView.DrawMode.ERASE) {
                binding.drawingView.setStrokeWidth(width * 2)
            }
        }
        dialog.show()
    }

    private fun updateUndoRedoButtons() {
        binding.buttonUndo.isEnabled = binding.drawingView.canUndo()
        binding.buttonUndo.alpha = if (binding.drawingView.canUndo()) 1.0f else 0.5f

        binding.buttonRedo.isEnabled = binding.drawingView.canRedo()
        binding.buttonRedo.alpha = if (binding.drawingView.canRedo()) 1.0f else 0.5f
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Save before exiting
                saveDrawing(true)
                finish()
                true
            }
            R.id.action_share -> {
                showShareDialog()
                true
            }
            R.id.action_collaborate -> {
                showCollaborateDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showShareDialog() {
        val qrCodeFragment = QRCodeFragment.newInstance(noteId, false)
        qrCodeFragment.show(supportFragmentManager, "qrcode_dialog")
    }

    private fun showCollaborateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_collaborator, null)
        val editTextEmail = dialogView.findViewById<android.widget.EditText>(R.id.et_email)

        AlertDialog.Builder(this)
            .setTitle("Add Collaborator")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val email = editTextEmail.text.toString().trim()
                if (email.isNotEmpty()) {
                    addCollaborator(email)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addCollaborator(email: String) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE

            try {
                val result = repository.shareNoteWithUser(noteId, email)

                if (result.isSuccess) {
                    Toast.makeText(this@NoteDetailActivity, "Collaborator added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@NoteDetailActivity, "Failed to add collaborator", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoteDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Save drawing state - use a specific job that won't be cancelled
        val saveJob = lifecycleScope.launch {
            try {
                saveDrawing(false)
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error saving drawing in onPause", e)
            }
        }

        // Wait for the save to complete (optional)
        runBlocking {
            try {
                saveJob.join()
            } catch (e: Exception) {
                Log.e("NoteDetailActivity", "Error waiting for save job", e)
            }
        }

        // Update user typing status
        collaborationManager.setUserTyping(false)
    }


    override fun onDestroy() {
        try {
            // Ensure we clean up properly
            binding.saveProgressBar.visibility = View.GONE
            binding.progressBar.visibility = View.GONE

            // Cleanup resources
            collaborationManager.cleanup()
        } catch (e: Exception) {
            Log.e("NoteDetailActivity", "Error during cleanup", e)
        }
        super.onDestroy()
    }
}