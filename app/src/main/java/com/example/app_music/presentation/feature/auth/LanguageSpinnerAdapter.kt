package com.example.app_music.presentation.feature.auth

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.app_music.R
import com.example.app_music.domain.utils.Language

class LanguageSpinnerAdapter(
    context: Context,
    private val languages: List<Language>
) : ArrayAdapter<Language>(context, R.layout.spinner_item_language, languages) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.spinner_item_language, parent, false)

        val language = getItem(position)
        val textView = view.findViewById<TextView>(R.id.tv_language_name)
        textView.text = language?.name


        val iconView = view.findViewById<ImageView>(R.id.iv_language_icon)


        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.spinner_dropdown_item_language, parent, false)

        val language = getItem(position)
        val textView = view.findViewById<TextView>(R.id.tv_language_name)
        textView.text = language?.name


        val iconView = view.findViewById<ImageView>(R.id.iv_language_icon)


        return view
    }
}