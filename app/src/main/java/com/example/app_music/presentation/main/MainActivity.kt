package com.example.app_music.presentation.main

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.app_music.R
import com.example.app_music.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val navController = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.let {
            it as? NavHostFragment
        }?.navController

        // Gán Navigation vào BottomNavigationView
        if (navController != null) {
            binding.bottomNavigationView.setupWithNavController(navController)
        } else {
            Log.e("MainActivity", "NavController is null!")
        }
    }
}