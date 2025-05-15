package com.example.app_music.presentation.feature.auth

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.app_music.domain.utils.Language

class LanguageSpinnerAdapter(
    context: Context,
    private val languages: List<Language>
) : ArrayAdapter<Language>(context, android.R.layout.simple_spinner_item, languages) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_item, parent, false)

        val language = getItem(position)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = language?.name

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)

        val language = getItem(position)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = language?.name

        return view
    }
}