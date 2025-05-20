package com.example.app_music.presentation.feature.menu

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.app_music.databinding.FragmentMenuBinding
import com.example.app_music.presentation.feature.menu.profile.ProfileActivity
import com.example.app_music.presentation.feature.menu.setting.SettingActivity
import com.example.app_music.R
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.presentation.feature.camera.CameraActivity
import com.example.app_music.presentation.feature.menu.premiumuser.PremiumUser
import com.example.app_music.presentation.feature.menu.transactions.TransactionHistoryActivity
import com.example.app_music.presentation.feature.noteScene.NoteActivity


class MenuFragment : Fragment() {
   private var _binding : FragmentMenuBinding ?= null
    private val binding get()= _binding!!

    private lateinit var  viewModel: MenuViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MenuViewModel::class.java]


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setUpObserver()
        val userId = UserPreference.getUserId(requireContext())
        viewModel.fetchUserData(userId)

    }

    override fun onResume() {
        val userId = UserPreference.getUserId(requireContext())
        viewModel.fetchUserData(userId)
        super.onResume()
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun setupClickListeners() {
        binding.btnSetting.setOnClickListener {
            val intent = Intent(requireContext(), SettingActivity::class.java)
            startActivity(intent)
        }

        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }
        binding.layoutPremium.setOnClickListener {
            val intent = Intent(requireContext(), PremiumUser::class.java)
            startActivity(intent)
        }
        binding.layoutTransactionHistory.setOnClickListener {
            val intent = Intent(requireContext(), TransactionHistoryActivity::class.java)
            startActivity(intent)
        }
        binding.textViewNote.setOnClickListener {
            val intent = Intent(requireContext(), NoteActivity::class.java)
            startActivity(intent)
        }
        binding.textViewSearch.setOnClickListener {
            val intent = Intent(requireContext(), CameraActivity::class.java)
            startActivity(intent)
        }

    }

    private fun setUpObserver()
    {
        viewModel.user.observe(viewLifecycleOwner) {user ->
            binding.textViewUserName.text = user.username ?: "User"
            if (!user.avatarUrl.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(user.avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .centerCrop()
                    .into(binding.imageViewProfileIcon)
            } else {

                binding.imageViewProfileIcon.setImageResource(R.drawable.ic_person)
            }
            updatePremiumSection(user.userRank)

        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility= if(isLoading) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
        }

    }
    private fun updatePremiumSection(userRank: String?) {
        when (userRank?.lowercase()) {
             "premium" -> {

                binding.layoutPremium.visibility = View.GONE
                binding.layoutTransactionHistory.visibility = View.VISIBLE

                 binding.textViewRecommendTitle.text = getString(R.string.transaction_history)



            }
            else -> {

                binding.layoutPremium.visibility = View.VISIBLE
                binding.layoutTransactionHistory.visibility = View.GONE
            }
        }
    }

}