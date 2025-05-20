package com.example.app_music.presentation.feature.noteScene

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class NoteFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Start NoteActivity immediately
        startActivity(Intent(requireContext(), NoteActivity::class.java))
        
        // Return an empty view since we're not actually showing this fragment
        return View(requireContext())
    }
}