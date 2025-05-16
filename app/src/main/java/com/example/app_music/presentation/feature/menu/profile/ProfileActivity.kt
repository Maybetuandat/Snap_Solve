package com.example.app_music.presentation.feature.menu.profile

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.app_music.R
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.databinding.ActivityProfileBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.menu.editstatusmessage.EditStatusMessageActivity
import com.example.app_music.presentation.feature.menu.editstudentInformation.EditStudentInformation
import com.example.app_music.presentation.feature.menu.editusernamescreen.EditUserNameActivity
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: ProfileEditViewModel
    private var selectedImageUri: Uri? = null


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Quyền đọc bộ nhớ bị từ chối", Toast.LENGTH_SHORT).show()
        }
    }
    private fun checkAndRequestPermissions() {
        when {
            // Android 13+ (API 33+): Sử dụng READ_MEDIA_IMAGES
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                        // Hiển thị dialog giải thích tại sao cần quyền này
                        showPermissionRationaleDialog(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                    else -> {
                        // Xin quyền
                        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
            }

            // Android 10+ (API 29+) đến Android 12
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                        // Hiển thị dialog giải thích tại sao cần quyền này
                        showPermissionRationaleDialog(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    else -> {
                        // Xin quyền
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }

            // Android 9 và thấp hơn
            else -> {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                        // Hiển thị dialog giải thích tại sao cần quyền này
                        showPermissionRationaleDialog(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    else -> {
                        // Xin quyền
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
    }

    // Hiển thị dialog giải thích về quyền
    private fun showPermissionRationaleDialog(permission: String) {
        AlertDialog.Builder(this)
            .setTitle("Cần quyền truy cập")
            .setMessage("Ứng dụng cần quyền truy cập vào thư viện ảnh để bạn có thể chọn ảnh đại diện.")
            .setPositiveButton("Cấp quyền") { _, _ ->
                requestPermissionLauncher.launch(permission)
            }
            .setNegativeButton("Không") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Không thể chọn ảnh khi không có quyền", Toast.LENGTH_LONG).show()
            }
            .create()
            .show()
    }

    // Launcher để mở gallery và chọn ảnh
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                displaySelectedImage(uri)

                // Chuyển URI thành File và upload
                val file = uriToFile(uri)
                if (file != null) {
                    val userId = UserPreference.getUserId(this)
                    viewModel.uploadAvatar(userId, file)
                } else {
                    Toast.makeText(this, "Không thể đọc file ảnh", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ProfileEditViewModel::class.java]

        setupListeners()
        observeViewModel()

        // Tải thông tin người dùng khi màn hình được tạo
        val userId = UserPreference.getUserId(this)
        viewModel.fetchUserData(userId)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.flUsernameContainer.setOnClickListener {
            val intent = Intent(this, EditUserNameActivity::class.java)
            startActivity(intent)
        }

        binding.flStatusContainer.setOnClickListener {
            val intent = Intent(this, EditStatusMessageActivity::class.java)
            startActivity(intent)
        }

        binding.flStudentInfoContainer.setOnClickListener {
            val intent = Intent(this, EditStudentInformation::class.java)
            startActivity(intent)
        }

        // Xử lý sự kiện click vào ảnh đại diện
        binding.cvProfilePictureContainer.setOnClickListener {
            checkPermissionAndOpenGallery()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        viewModel.user.observe(this) { user ->
            // Hiển thị thông tin người dùng
            binding.tvUsernameValue.text = user.username
            binding.tvStatusValue.text = user.statusMessage ?: getString(R.string.detail_status_message)
            binding.tvStudentInfoValue.text = user.studentInformation ?: getString(R.string.detail_student_infomation)
            binding.tvEmailValue.text = user.email
            binding.tvUidValue.text = user.suid

            // Hiển thị ảnh đại diện nếu có
            if (!user.avatarUrl.isNullOrEmpty()) {
                displayAvatarFromUrl(user.avatarUrl!!)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Hiển thị loading indicator nếu cần
            if (isLoading) {
                // Hiển thị progress bar hoặc indicator
            } else {
                // Ẩn progress bar hoặc indicator
            }
        }

        viewModel.error.observe(this) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.avatarUploaded.observe(this) { isUploaded ->
            if (isUploaded) {
                Toast.makeText(this, "Ảnh đại diện đã được cập nhật", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionAndOpenGallery() {
        checkAndRequestPermissions()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun displaySelectedImage(uri: Uri) {
        // Xóa TextView "Change" nếu có
        binding.cvProfilePictureContainer.removeAllViews()

        // Tạo ImageView mới và thêm vào CardView
        val imageView = ImageView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        binding.cvProfilePictureContainer.addView(imageView)

        // Hiển thị ảnh đã chọn
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(imageView)
    }

    private fun displayAvatarFromUrl(avatarUrl: String) {
        // Xóa TextView "Change" nếu có
        binding.cvProfilePictureContainer.removeAllViews()

        // Tạo ImageView mới và thêm vào CardView
        val imageView = ImageView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        binding.cvProfilePictureContainer.addView(imageView)

        // Hiển thị ảnh từ URL
        Glide.with(this)
            .load(avatarUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_person) // Ảnh placeholder khi đang tải
            .error(R.drawable.ic_person) // Ảnh hiển thị khi lỗi
            .into(imageView)
    }

    // Helper method để chuyển đổi từ Uri sang File
    private fun uriToFile(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputFile = File(cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(4 * 1024) // 4k buffer
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }


}