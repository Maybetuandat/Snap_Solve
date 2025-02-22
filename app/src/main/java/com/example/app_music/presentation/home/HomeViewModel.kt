package com.example.app_music.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel  @Inject constructor():ViewModel(){
    private val _text = MutableLiveData("This is home fragment")
    val text : LiveData<String> = _text
    fun updateText(newText : String)
    {
        _text.value = newText
    }
}