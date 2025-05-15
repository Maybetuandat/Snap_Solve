package com.example.app_music.presentation.feature.menu

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.repository.UserRepository
import com.example.app_music.domain.model.User
import kotlinx.coroutines.launch
import retrofit2.Response

class MenuViewModel(): ViewModel() {
    private val repository = UserRepository()
    private val _user = MutableLiveData<User>()
    val user : LiveData<User> = _user


    private val _idLoading = MutableLiveData<Boolean>()
    val isLoading : LiveData<Boolean> = _idLoading


    private val _error = MutableLiveData<String>()
    val error : LiveData<String> = _error

    fun fetchUserData(userId : Long)
    {
        _idLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getUserById(userId)
                handleUserResponse(response)
            }
            catch (e : Exception)
            {
                Log.e("menuviewmodel", e.toString())
                _error.value = "error with fetching user data " + e.toString()
            }
            finally {
                _idLoading.value = false
            }
        }
    }
    private fun handleUserResponse(response: Response<User>) {

        if(response.isSuccessful)
        {
            response.body()?.let {
                _user.value = it
            } ?: run {
                _error.value= "User data is empty"
            }
        }
        else{
            _error.value = "Error: ${response.code()} - ${response.message()}"
        }
    }
}