package com.example.app_music.data.repository



import com.example.app_music.data.remote.api.AuthApi
import com.example.app_music.domain.model.AuthResponse
import com.example.app_music.domain.model.User
import com.example.app_music.domain.repository.AuthRepository
import com.example.app_music.domain.utils.RetrofitFactory.authApi
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import retrofit2.Response


class AuthRepository(
    private val apiService: AuthApi,

) : AuthRepository {

    override suspend fun login(username: String, password: String): Response<AuthResponse> {
        val response = apiService.login(username, password)
        if(response.isSuccessful) {
            val authResponse = response.body()!!
            return Response.success(authResponse)

        }
        else {


            return Response.error(
                response.code(),
                ResponseBody.create(
                    "text/plain".toMediaTypeOrNull(),
                    response.errorBody()?.string() ?: "Unknown error"
                )
            )
        }
    }

    override suspend fun register(
    user : User
    ): Response<User> {


        val response = apiService.register(user)
        if(response.isSuccessful)
        {
            val user = response.body()
            return Response.success(user)
        }
        else
        {
            return Response.error(
                response.code(),
                ResponseBody.create(
                    "text/plain".toMediaTypeOrNull(),
                    response.errorBody()?.string() ?: "Unknown error"
                )
            )
        }

    }



}