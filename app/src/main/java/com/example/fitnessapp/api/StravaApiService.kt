package com.example.fitnessapp.api

import com.example.fitnessapp.model.StravaActivity
import com.example.fitnessapp.model.StravaAthlete
import com.example.fitnessapp.model.StravaToken
import com.example.fitnessapp.model.SyncResult
import retrofit2.Call
import retrofit2.http.*

interface StravaApiService {
    @GET("strava/connect-mobile")
    suspend fun getAuthUrl(): Map<String, String>

    @GET("strava/callback")
    fun exchangeCodeForToken(
        @Header("Authorization") jwtToken: String,
        @Query("code") code: String
    ): Call<StravaToken>

    @GET("strava/athlete")
    fun getAthlete(
        @Header("Authorization") jwtToken: String
    ): Call<StravaAthlete>

    @GET("strava/activities")
    fun getActivities(
        @Header("Authorization") jwtToken: String,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): Call<List<StravaActivity>>

    @POST("strava/refresh-token")
    fun refreshToken(
        @Header("Authorization") jwtToken: String
    ): Call<StravaToken>

    @GET("strava/activities-by-sport")
    suspend fun getActivitiesBySport(
        @Header("Authorization") jwtToken: String
    ): Map<String, List<StravaActivity>>

    @GET("strava/sync")
    suspend fun syncStravaActivities(
        @Header("Authorization") jwtToken: String
    ): retrofit2.Response<SyncResult>

    @GET("strava/activities-db")
    suspend fun getStravaActivitiesFromDb(
        @Header("Authorization") jwtToken: String
    ): retrofit2.Response<List<StravaActivity>>
} 