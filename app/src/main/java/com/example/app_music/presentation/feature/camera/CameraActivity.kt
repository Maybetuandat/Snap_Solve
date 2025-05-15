package com.example.app_music.presentation.feature.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.app_music.R
import com.example.app_music.databinding.ActivityCameraBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : BaseActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var photoFile: File? = null
    private var originalBitmap: Bitmap? = null

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImageUri ->
            try {
                val inputStream = contentResolver.openInputStream(selectedImageUri)
                originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                // Show in preview with proper scaling
                showImagePreview(originalBitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error loading image: ${e.message}")
            }
        }
    }

    private fun showImagePreview(bitmap: Bitmap?) {
        bitmap?.let {
            // Show preview layout and hide camera layout
            binding.cameraLayout.visibility = View.GONE
            binding.previewLayout.visibility = View.VISIBLE

            // Reset image scale type for proper display
            binding.imagePreview.scaleType = ImageView.ScaleType.FIT_CENTER
            binding.imagePreview.setImageBitmap(bitmap)

            // Enable area selection
            binding.cropOverlay.visibility = View.VISIBLE

            // Initialize crop rect based on image size after a short delay to ensure view is laid out
            binding.imagePreview.post {
                binding.cropOverlay.initializeCropRectForBitmap(
                    bitmap.width,
                    bitmap.height,
                    binding.imagePreview.width,
                    binding.imagePreview.height
                )
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var permissionsGranted = true

        permissions.entries.forEach {
            if (!it.value) {
                permissionsGranted = false
                return@forEach
            }
        }

        if (permissionsGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera and Storage permissions are required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check and request permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestCameraPermissions()
        }

        // Set up button click listeners
        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        binding.btnGallery.setOnClickListener {
            openGallery()
        }

        binding.btnRotate.setOnClickListener {
            rotateImage()
        }

        binding.btnRetry.setOnClickListener {
            binding.previewLayout.visibility = View.GONE
            binding.cameraLayout.visibility = View.VISIBLE
        }

        binding.btnConfirm.setOnClickListener {
            processSelectedArea()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnBackPreview.setOnClickListener {
            binding.previewLayout.visibility = View.GONE
            binding.cameraLayout.visibility = View.VISIBLE
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider = cameraProviderFuture.get()

            try {
                // Preview
                val preview = Preview.Builder()
                    .build()

                // Set the surface provider differently based on CameraX version
                val previewView = binding.cameraPreviewView

                // Using the proper way to set surface provider based on the CameraX version
                try {
                    // For newer CameraX versions
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                } catch (e: Exception) {
                    try {
                        // For older CameraX versions using reflection
                        val method = preview.javaClass.getMethod("setSurfaceProvider", java.util.concurrent.Executor::class.java, Preview.SurfaceProvider::class.java)
                        method.invoke(preview, ContextCompat.getMainExecutor(this), previewView.surfaceProvider)
                    } catch (e2: Exception) {
                        // If both approaches fail, log and show error
                        Log.e(TAG, "Failed to set surface provider: ${e2.message}")
                        Toast.makeText(this, "Camera initialization error. Please update your app.", Toast.LENGTH_SHORT).show()
                    }
                }

                // Image capture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Failed to initialize camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create file for the image
        photoFile = createImageFile()

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile!!)
            .build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Load the captured image
                    originalBitmap = BitmapFactory.decodeFile(photoFile?.absolutePath)

                    // Show in preview
                    showImagePreview(originalBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(baseContext, "Failed to capture image: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun createImageFile(): File {
        // Create a unique filename
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(null)

        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }

    private fun rotateImage() {
        originalBitmap?.let { bitmap ->
            val matrix = Matrix().apply { postRotate(90f) }
            originalBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            // Show the rotated image properly
            showImagePreview(originalBitmap)
        }
    }

    private fun processSelectedArea() {
        // Get the selected area coordinates from the CropOverlayView
        val selectedRect = binding.cropOverlay.getSelectedRect()

        if (selectedRect == null || originalBitmap == null) {
            Toast.makeText(this, "Please select an area first", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Get the ImageView dimensions
            val viewWidth = binding.imagePreview.width.toFloat()
            val viewHeight = binding.imagePreview.height.toFloat()

            // Get bitmap dimensions
            val bitmapWidth = originalBitmap!!.width.toFloat()
            val bitmapHeight = originalBitmap!!.height.toFloat()

            // Calculate the scale factor between the ImageView and the bitmap
            // This is needed to convert between view coordinates and bitmap coordinates
            val scaleX = bitmapWidth / viewWidth
            val scaleY = bitmapHeight / viewHeight

            // If the image is centered in the view (e.g., due to centerInside scaleType)
            // we need to calculate offsets
            val imageViewRatio = viewWidth / viewHeight
            val bitmapRatio = bitmapWidth / bitmapHeight

            var offsetX = 0f
            var offsetY = 0f

            if (imageViewRatio > bitmapRatio) {
                // Image is taller than the view, centered horizontally
                val scaledWidth = viewHeight * bitmapRatio
                offsetX = (viewWidth - scaledWidth) / 2f
            } else {
                // Image is wider than the view, centered vertically
                val scaledHeight = viewWidth / bitmapRatio
                offsetY = (viewHeight - scaledHeight) / 2f
            }

            // Convert touch coordinates to bitmap coordinates
            val cropX = (selectedRect.left - offsetX) * scaleX
            val cropY = (selectedRect.top - offsetY) * scaleY
            val cropWidth = selectedRect.width() * scaleX
            val cropHeight = selectedRect.height() * scaleY

            // Log coordinates for debugging
            Log.d(TAG, "ImageView size: $viewWidth x $viewHeight")
            Log.d(TAG, "Bitmap size: $bitmapWidth x $bitmapHeight")
            Log.d(TAG, "Scale: $scaleX x $scaleY, Offset: $offsetX x $offsetY")
            Log.d(TAG, "Selected in view: $selectedRect")
            Log.d(TAG, "Crop in bitmap: $cropX, $cropY, $cropWidth x $cropHeight")

            // Ensure crop values are within bitmap bounds
            val safeX = cropX.coerceIn(0f, bitmapWidth - 1f)
            val safeY = cropY.coerceIn(0f, bitmapHeight - 1f)

            // Fix the ambiguity by explicitly using intermediate variables
            val widthRemaining = bitmapWidth - safeX
            val heightRemaining = bitmapHeight - safeY
            val safeWidth = cropWidth.coerceIn(1f, widthRemaining)
            val safeHeight = cropHeight.coerceIn(1f, heightRemaining)

            // Crop the bitmap to the selected area
            val croppedBitmap = Bitmap.createBitmap(
                originalBitmap!!,
                safeX.toInt(),
                safeY.toInt(),
                safeWidth.toInt(),
                safeHeight.toInt()
            )

            // Save the cropped bitmap to a file
            val croppedFile = saveBitmapToFile(croppedBitmap)

            // Start the result activity
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("IMAGE_PATH", croppedFile?.absolutePath)
            }
            startActivity(intent)

        } catch (e: Exception) {
            Log.e(TAG, "Error during image crop: ${e.message}", e)
            Toast.makeText(this, "Error cropping image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "CROPPED_${timeStamp}.jpg"
            val storageDir = getExternalFilesDir(null)
            val imageFile = File(storageDir, imageFileName)

            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            imageFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"

        private val REQUIRED_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            } else {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
    }
}