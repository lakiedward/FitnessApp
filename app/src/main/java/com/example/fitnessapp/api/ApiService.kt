package com.example.fitnessapp.api


import com.example.fitnessapp.model.RacesModelResponse
import com.example.fitnessapp.model.SportsSelectionRequest
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.model.TrainingPlanGenerate
import com.example.fitnessapp.model.User
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.model.UserRaces
import com.example.fitnessapp.model.UserTrainigData
import com.example.fitnessapp.model.UserWeekAvailability
import com.example.fitnessapp.model.SyncResult
import com.example.fitnessapp.model.StravaActivity
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*



interface ApiService {

    @POST("users/create_user")
    fun createUser(
        @Body user: User
    ): Call<Map<String, String>>

    @POST("users/token")
    fun login(
        @Body user: User
    ): Call<Map<String, String>>

    @POST("training/add_user_details")
    fun addUserDetails(
        @Header("Authorization") token: String,
        @Body details: UserDetalis
    ): Call<Map<String, String>>

    @POST("availability/add_week_availability")
    fun addWeekAvailability(
        @Header("Authorization") token: String,
        @Body availabilityList: List<UserWeekAvailability>
    ): Call<Map<String, String>>

    @POST("training/add_or_update_training_data")
    fun addOrUpdateTrainingData(
        @Header("Authorization") token: String,
        @Body trainingData: UserTrainigData
    ): Call<Map<String, String>>

    @GET("training/user_training_data")
    fun getUserTrainingData(
        @Header("Authorization") token: String
    ): Call<UserTrainigData>

    @POST("race/add_race")
    fun addRace(
        @Header("Authorization") token: String,
        @Body races: UserRaces
    ): Call<Map<String, String>>

    @POST("api/generate_cycling_plan")
    fun generateCyclingPlan(
        @Header("Authorization") token: String,
        @Body trainingPlanGenerate: TrainingPlanGenerate
    ): Call<Map<String, String>>

    @POST("running/save")
    suspend fun saveRunningData(
        @Header("Authorization") token: String,
        @Body runningData: Map<String, Int?>
    ): Response<Void>

    @POST("sports/select")
    fun selectUserSports(
        @Header("Authorization") token: String,
        @Body request: SportsSelectionRequest
    ): Call<Void>

    @POST("plan/generate_based_on_sport")
    fun generateTrainingPlanBySport(
        @Header("Authorization") token: String,
        @Body body: Map<String, String> // conține doar "race_date"
    ): Call<Map<String, String>>

    @GET("training/get_training_plan")
    fun getTrainingPlan(@Header("Authorization") token: String): Call<List<TrainingPlan>>


    @GET("race/get_races")
    fun getRaces(@Header("Authorization") token: String): Call<RacesModelResponse>

}
