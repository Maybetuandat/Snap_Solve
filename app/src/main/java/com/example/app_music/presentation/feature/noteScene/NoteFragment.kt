package com.example.app_music.presentation.feature.noteScene

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.app_music.R

class NoteFragment : Fragment() {

    private var hasStartedActivity = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Start NoteActivity immediately
        startActivity(Intent(requireContext(), NoteActivity::class.java))
        hasStartedActivity = true

        // Return an empty view since we're not actually showing this fragment
        return View(requireContext())
    }

    override fun onResume() {
        super.onResume()

        // Khi fragment này resume sau khi quay về từ NoteActivity
        if (hasStartedActivity) {
            // Chuyển về HomeFragment
            findNavController().navigate(R.id.homeFragment)
            hasStartedActivity = false
        }
    }
}