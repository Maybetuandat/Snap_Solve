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

                    else -> {
                        // Xin quyền
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
    }



    // Launcher để mở gallery và chọn ảnh
    //android mo ung dung cho phep chon anh -> nguoi dung lua chon -> android se tra ve uri( con tro tro den anh)
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri


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


        binding.cvProfilePictureContainer.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        viewModel.user.observe(this) { user ->
            // Hiển thị thông tin người dùng
            binding.tvUsernameValue.text = user.username
            binding.tvStatusValue.text = user.statusMessage
            binding.tvStudentInfoValue.text = user.studentInformation
            binding.tvEmailValue.text = user.email
            binding.tvUidValue.text = user.suid


            if (!user.avatarUrl.isNullOrEmpty()) {
                displayAvatarFromUrl(user.avatarUrl!!)
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



    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }



    private fun displayAvatarFromUrl(avatarUrl: String) {

        binding.cvProfilePictureContainer.removeAllViews()


        val imageView = ImageView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        binding.cvProfilePictureContainer.addView(imageView)


        Glide.with(this)
            .load(avatarUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(imageView)
    }


    private fun uriToFile(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputFile = File(cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(4 * 1024) // 4k buffer  //kich thuoc cua mot block -> doc theo tung block mot trong anh
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
    override fun onResume() {
        super.onResume()
        val userId = UserPreference.getUserId(this)
        viewModel.fetchUserData(userId)
    }


}