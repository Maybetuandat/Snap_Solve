package com.example.app_music.domain.usecase.user




import android.util.Log
import com.example.app_music.domain.model.User
import com.example.app_music.domain.repository.UserRepository
import retrofit2.Response

class UpdateUserRankUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: Long, newRank: String): Response<User> {
        return try {
            Log.d("UpdateUserRankUseCase", "Updating user rank for userId: $userId to rank: $newRank")


            val getUserResponse = repository.getUserById(userId)

            if (getUserResponse.isSuccessful && getUserResponse.body() != null) {
                val currentUser = getUserResponse.body()!!
                //Log.d("UpdateUserRankUseCase", "Current user: $currentUser")


                val updatedUser = currentUser.copy(userRank = newRank)
                //Log.d("UpdateUserRankUseCase", "Updated user: $updatedUser")


                val updateResponse = repository.updateUser(userId, updatedUser)

                if (updateResponse.isSuccessful) {
                    Log.d("UpdateUserRankUseCase", "User rank updated successfully")
                } else {
                    Log.e("UpdateUserRankUseCase", "Failed to update user rank: ${updateResponse.message()}")
                }

                updateResponse
            } else {
                Log.e("UpdateUserRankUseCase", "Failed to get current user: ${getUserResponse.message()}")
                getUserResponse
            }
        } catch (e: Exception) {
            Log.e("UpdateUserRankUseCase", "Error updating user rank", e)
            throw e
        }
    }
}