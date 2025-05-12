package com.example.app_music

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.app_music.databinding.ActivityMainBinding
import com.example.app_music.domain.utils.MultiLanguage
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.setting.restartappdialog.RestartAppDialog

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun attachBaseContext(newBase: Context) {

        val languageCode = MultiLanguage.getSelectedLanguage(newBase)
        val context = MultiLanguage.applyLanguage(newBase, languageCode)
        super.attachBaseContext(context)
    }
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



        checkLanguageChanged()
    }
    private fun checkLanguageChanged() {
        if (MultiLanguage.isLanguageChanged(this)) {

            RestartAppDialog(this).show()


            MultiLanguage.resetLanguageChanged(this)
        }
    }
}