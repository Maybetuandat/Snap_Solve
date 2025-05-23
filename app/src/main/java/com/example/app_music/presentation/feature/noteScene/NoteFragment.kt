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
        return View(requireContext())
    }

    override fun onResume() {
        super.onResume()

        // khi back về thì chuyển về home
        if (hasStartedActivity) {
            findNavController().navigate(R.id.homeFragment)
            hasStartedActivity = false
        }
    }
}