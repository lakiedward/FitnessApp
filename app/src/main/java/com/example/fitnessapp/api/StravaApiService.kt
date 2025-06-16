package com.example.fitnessapp.api

import com.example.fitnessapp.model.StravaActivity
import com.example.fitnessapp.model.StravaAthlete
import com.example.fitnessapp.model.StravaToken
import com.example.fitnessapp.model.SyncResult
import com.example.fitnessapp.model.FTPEstimate
import retrofit2.Call
import retrofit2.http.*

interface StravaApiService {
    @GET("strava/connect-mobile")
    suspend fun getAuthUrl(
        @Header("Authorization") jwtToken: String
    ): Map<String, String>

    @GET("strava/callback")
    fun exchangeCodeForToken(
        @Header("Authorization") jwtToken: String,
        @Query("code") code: String
    ): Call<StravaToken>

    @GET("strava/athlete")
    fun getAthlete(
        @Header("Authorization") jwtToken: String,
        @Header("X-Strava-Token") stravaToken: String
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

    @GET("strava/sync-check")
    suspend fun syncCheck(
        @Header("Authorization") jwtToken: String
    ): retrofit2.Response<SyncResult>

    @GET("strava/sync-live")
    suspend fun syncLive(
        @Header("Authorization") jwtToken: String
    ): retrofit2.Response<String>

    @GET("strava/ftp-estimate")
    suspend fun estimateFtp(
        @Header("Authorization") jwtToken: String,
        @Query("days") days: Int = 30
    ): FTPEstimate

    @GET("strava/ftp-estimate-db")
    suspend fun getLastFtpEstimateFromDb(
        @Header("Authorization") jwtToken: String
    ): FTPEstimate

    @GET("strava/estimate-cycling-fthr")
    suspend fun estimateCyclingFthr(
        @Header("Authorization") jwtToken: String,
        @Query("days") days: Int = 90
    ): retrofit2.Response<Map<String, Any>>

    @GET("strava/estimate-cycling-fthr-20min")
    suspend fun estimateCyclingFthr20min(
        @Header("Authorization") jwtToken: String,
        @Query("days") days: Int = 90
    ): retrofit2.Response<Map<String, Any>>

    @POST("strava/set-fthr")
    suspend fun setCyclingFthr(
        @Header("Authorization") jwtToken: String,
        @Body body: Map<String, Any>
    ): retrofit2.Response<Map<String, Any>>

    @GET("strava/estimate-running-fthr")
    suspend fun estimateRunningFthr(
        @Header("Authorization") jwtToken: String,
        @Query("days") days: Int = 90
    ): retrofit2.Response<Map<String, Any>>

    @GET("strava/estimate-swimming-fthr")
    suspend fun estimateSwimmingFthr(
        @Header("Authorization") jwtToken: String,
        @Query("days") days: Int = 90
    ): retrofit2.Response<Map<String, Any>>

    @GET("strava/estimate-other-fthr")
    suspend fun estimateOtherFthr(
        @Header("Authorization") jwtToken: String,
        @Query("days") days: Int = 90
    ): retrofit2.Response<Map<String, Any>>

    @POST("strava/calculate-hrtss")
    suspend fun calculateHrTss(
        @Header("Authorization") jwtToken: String
    ): retrofit2.Response<Map<String, Any>>
} 