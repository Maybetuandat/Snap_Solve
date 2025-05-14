//package com.example.app_music.data.repository
//
//class AuthRepositoryImpl(
//    private val apiService: ApiService,
//    private val preferencesDataSource: PreferencesDataSource
//) : AuthRepository {
//
//    override suspend fun login(username: String, password: String): Response<AuthResponse> {
//        val response = apiService.login(username, password)
//        return if (response.isSuccessful && response.body() != null) {
//            Response.success(Mappers.mapAuthResponseDtoToDomain(response.body()!!))
//        } else {
//            Response.error(response.code(), response.errorBody()!!)
//        }
//    }
//
//    override suspend fun register(
//        username: String,
//        email: String,
//        phoneNumber: String,
//        password: String
//    ): Response<User> {
//        // Store password temporarily for auto-login after registration
//        preferencesDataSource.saveTempPassword(password)
//
//        val userCreateDto = Mappers.createUserCreateDto(username, email, phoneNumber, password)
//        val response = apiService.register(userCreateDto)
//
//        return if (response.isSuccessful && response.body() != null) {
//            Response.success(Mappers.mapUserDtoToDomain(response.body()!!))
//        } else {
//            Response.error(response.code(), response.errorBody()!!)
//        }
//    }
//
//    override suspend fun updateProfile(
//        userId: Long,
//        firstName: String,
//        lastName: String,
//        dob: LocalDate
//    ): Response<User> {
//        val userUpdateDto = Mappers.createUserUpdateDto(firstName, lastName, dob)
//        val response = apiService.updateUser(userId, userUpdateDto)
//
//        return if (response.isSuccessful && response.body() != null) {
//            Response.success(Mappers.mapUserDtoToDomain(response.body()!!))
//        } else {
//            Response.error(response.code(), response.errorBody()!!)
//        }
//    }
//
//    override fun saveAuthToken(token: String) {
//        preferencesDataSource.saveAuthToken(token)
//    }
//
//    override fun getAuthToken(): String? {
//        return preferencesDataSource.getAuthToken()
//    }
//
//    override fun saveUserData(user: User) {
//        preferencesDataSource.saveUserData(Mappers.mapUserDomainToDto(user))
//    }
//
//    override fun getUserData(): User? {
//        val userDto = preferencesDataSource.getUserData()
//        return userDto?.let { Mappers.mapUserDtoToDomain(it) }
//    }
//
//    override fun clearUserSession() {
//        preferencesDataSource.clearAll()
//    }
//
//    override fun getStoredPassword(): String {
//        return preferencesDataSource.getTempPassword() ?: ""
//    }
//}