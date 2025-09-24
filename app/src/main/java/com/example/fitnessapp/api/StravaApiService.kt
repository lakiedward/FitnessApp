package com.example.fitnessapp.api

import com.example.fitnessapp.model.FTPEstimate
import com.example.fitnessapp.model.StravaActivity
import com.example.fitnessapp.model.StravaAthlete
import com.example.fitnessapp.model.StravaToken
import com.example.fitnessapp.model.SyncResult
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("strava/disconnect")
    suspend fun disconnectStravaAccount(
        @Header("Authorization") jwtToken: String
    ): retrofit2.Response<Map<String, String>>

    @GET("running/pace-prediction")
    suspend fun getRunningPacePrediction(
        @Header("Authorization") jwtToken: String
    ): retrofit2.Response<List<Map<String, Any>>>

    @GET("swim/best-time-prediction")
    suspend fun getSwimBestTimePrediction(
        @Header("Authorization") jwtToken: String
    ): retrofit2.Response<List<Map<String, Any>>>

    @GET("strava/activities-db")
    suspend fun getActivitiesFromDb(
        @Header("Authorization") jwtToken: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): retrofit2.Response<List<StravaActivity>>

    @GET("strava/activities/{activity_id}/gpx")
    suspend fun getActivityGpx(
        @Header("Authorization") jwtToken: String,
        @Path("activity_id") activityId: Long
    ): retrofit2.Response<Map<String, String>>

    @GET("strava/activities/{activity_id}/map-view")
    suspend fun getActivityMapView(
        @Header("Authorization") jwtToken: String,
        @Path("activity_id") activityId: Long
    ): retrofit2.Response<Map<String, String>>

    @GET("strava/activity/{activity_id}/streams-db")
    suspend fun getActivityStreamsFromDB(
        @Header("Authorization") jwtToken: String,
        @Path("activity_id") activityId: Long
    ): retrofit2.Response<Map<String, Any>>

    @GET("strava/activity/{activity_id}/power-curve")
    suspend fun getActivityPowerCurve(
        @Header("Authorization") jwtToken: String,
        @Path("activity_id") activityId: Long
    ): retrofit2.Response<com.example.fitnessapp.model.PowerCurveResponse>

    @GET("strava/activities-unified")
    suspend fun getUnifiedActivities(
        @Header("Authorization") jwtToken: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): retrofit2.Response<List<StravaActivity>>

    @GET("strava/max-bpm")
    suspend fun getMaxBpm(
        @Header("Authorization") jwtToken: String
    ): retrofit2.Response<Map<String, Any>>

    @GET("strava/gear/{id}")
    suspend fun getGearDetails(
        @Header("Authorization") jwtToken: String,
        @Path("id") gearId: String
    ): com.example.fitnessapp.model.StravaGear
}
