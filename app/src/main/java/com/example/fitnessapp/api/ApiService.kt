package com.example.fitnessapp.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

data class User(val username: String, val password: String)
data class UpdateGender(val username: String, val gender: String)

interface ApiService {

    @POST("create_user")
    fun createUser(@Body user: User): Call<Map<String, String>>

    @POST("login")
    fun login(@Body user: User): Call<Map<String, String>>

    @PUT("update_gender")
    fun updateGender(@Body genderData: UpdateGender): Call<Map<String, String>>
}
