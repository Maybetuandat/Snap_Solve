package com.example.app_music.presentation.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.app_music.R
import com.example.app_music.databinding.ActivityProfileBinding
import com.example.app_music.presentation.editStatusMessage.EditStatusMessageActivity
import com.example.app_music.presentation.editStudentInformation.EditStudentInformation
import com.example.app_music.presentation.editUserNameScreen.EditUserNameActivity

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding : ActivityProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener{
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

    }
}