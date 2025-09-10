package com.example.fitnessapp.api


import com.example.fitnessapp.model.CyclingFtpResponse
import com.example.fitnessapp.model.DetaliiUserCycling
import com.example.fitnessapp.model.DeleteActivityResponse
import com.example.fitnessapp.model.EnhancedWorkoutRequest
import com.example.fitnessapp.model.EnhancedWorkoutResponse
import com.example.fitnessapp.model.HealthConnectActivity
import com.example.fitnessapp.model.HealthConnectStats
import com.example.fitnessapp.model.HealthConnectSyncRequest
import com.example.fitnessapp.model.HealthConnectSyncResponse
import com.example.fitnessapp.model.LastSyncResponse
import com.example.fitnessapp.model.ManualSyncRequest
import com.example.fitnessapp.model.OverreachingStatus
import com.example.fitnessapp.model.PacePredictions
import com.example.fitnessapp.model.PlanAdjustmentResponse
import com.example.fitnessapp.model.RacesModelResponse
import com.example.fitnessapp.model.RunningFtpResponse
import com.example.fitnessapp.model.SavedWorkout
import com.example.fitnessapp.model.SavedWorkoutResponse
import com.example.fitnessapp.model.SetupStatusResponse
import com.example.fitnessapp.model.SportsSelectionRequest
import com.example.fitnessapp.model.SwimPacePredictions
import com.example.fitnessapp.model.SwimmingPaceResponse
import com.example.fitnessapp.model.TrainingDateUpdate
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.model.TrainingPlanGenerate
import com.example.fitnessapp.model.TrainingStats
import com.example.fitnessapp.model.User
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.model.UserRaces
import com.example.fitnessapp.model.UserTrainigData
import com.example.fitnessapp.model.UserWeekAvailability
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("users/create_user")
    suspend fun createUser(
        @Body user: User
    ): Response<Map<String, String>>

    @POST("users/token")
    suspend fun login(
        @Body user: User
    ): Response<Map<String, String>>

    @POST("training/add_user_details")
    suspend fun addUserDetails(
        @Header("Authorization") token: String,
        @Body details: UserDetalis
    ): Response<Map<String, String>>

    @POST("availability/add_week_availability")
    suspend fun addWeekAvailability(
        @Header("Authorization") token: String,
        @Body availabilityList: List<UserWeekAvailability>
    ): Response<Map<String, String>>

    @POST("training/ftp/manual")
    suspend fun addOrUpdateCyclingData(
        @Header("Authorization") token: String,
        @Body cyclingData: DetaliiUserCycling
    ): Response<Map<String, String>>

    @GET("training/user_training_data")
    suspend fun getUserTrainingData(
        @Header("Authorization") token: String
    ): Response<UserTrainigData>

    @POST("race/add_race")
    suspend fun addRace(
        @Header("Authorization") token: String,
        @Body races: UserRaces
    ): Response<Map<String, String>>

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
    suspend fun selectUserSports(
        @Header("Authorization") token: String,
        @Body request: SportsSelectionRequest
    ): Response<Void>

    @POST("plan/generate_based_on_sport")
    suspend fun generateTrainingPlanBySport(
        @Header("Authorization") token: String,
        @Body body: Map<String, String> // conține doar "race_date"
    ): Response<Map<String, String>>

    @GET("training/get_training_plan")
    suspend fun getTrainingPlan(@Header("Authorization") token: String): Response<List<TrainingPlan>>


    @GET("race/get_races")
    suspend fun getRaces(@Header("Authorization") token: String): Response<RacesModelResponse>

    // New endpoints for setup status and manual data entry
    @GET("sports/setup-status")
    suspend fun getSetupStatus(
        @Header("Authorization") token: String
    ): Response<SetupStatusResponse>

    @POST("running/pace-prediction/manual")
    suspend fun addManualRunningPacePredictions(
        @Header("Authorization") token: String,
        @Body pacePredictions: PacePredictions
    ): Response<Map<String, String>>

    @POST("swim/best-time-prediction/manual")
    suspend fun addManualSwimPacePredictions(
        @Header("Authorization") token: String,
        @Body swimPacePredictions: SwimPacePredictions
    ): Response<Map<String, String>>

    @GET("swim/pace-100m")
    suspend fun getSwimmingPace(
        @Header("Authorization") token: String
    ): Response<SwimmingPaceResponse>

    @GET("running/ftp")
    suspend fun getRunningFtp(
        @Header("Authorization") token: String
    ): Response<RunningFtpResponse>

    @GET("cycling/ftp")
    suspend fun getCyclingFtp(
        @Header("Authorization") token: String
    ): Response<CyclingFtpResponse>

    @PUT("training/training_plan/{plan_id}/date")
    suspend fun updateTrainingPlanDate(
        @Path("plan_id") planId: Int,
        @Header("Authorization") token: String,
        @Body trainingDateUpdate: TrainingDateUpdate
    ): Response<Map<String, String>>

    // Health Connect endpoints
    @POST("health-connect/sync-activities")
    suspend fun syncHealthConnectActivities(
        @Header("Authorization") token: String,
        @Body syncRequest: HealthConnectSyncRequest
    ): Response<HealthConnectSyncResponse>

    @GET("health-connect/last-sync")
    suspend fun getLastHealthConnectSync(
        @Header("Authorization") token: String
    ): Response<LastSyncResponse>

    @POST("health-connect/manual-sync")
    suspend fun triggerManualHealthConnectSync(
        @Header("Authorization") token: String,
        @Body syncRequest: HealthConnectSyncRequest
    ): Response<HealthConnectSyncResponse>

    @GET("health-connect/activities")
    suspend fun getHealthConnectActivities(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<List<HealthConnectActivity>>

    @GET("health-connect/stats")
    suspend fun getHealthConnectStats(
        @Header("Authorization") token: String
    ): Response<HealthConnectStats>

    @DELETE("health-connect/activities/{activityId}")
    suspend fun deleteHealthConnectActivity(
        @Header("Authorization") token: String,
        @Path("activityId") activityId: String
    ): Response<DeleteActivityResponse>

    // Delete Strava Activity
    @DELETE("strava/activities/{strava_id}")
    suspend fun deleteStravaActivity(
        @Header("Authorization") token: String,
        @Path("strava_id") stravaId: Long
    ): Response<Map<String, String>>

    // Delete App Workout Activity  
    @DELETE("workout-execution/workouts/{workout_id}")
    suspend fun deleteAppWorkout(
        @Header("Authorization") token: String,
        @Path("workout_id") workoutId: Int
    ): Response<Map<String, String>>

    // App workouts endpoint
    @POST("workout-execution/workouts/save")
    suspend fun saveWorkout(
        @Header("Authorization") token: String,
        @Body workout: SavedWorkout
    ): Response<SavedWorkoutResponse>

    // Enhanced Workout Execution Endpoints
    @POST("workout-execution/workouts/save")
    suspend fun saveEnhancedWorkout(
        @Header("Authorization") token: String,
        @Body workout: EnhancedWorkoutRequest
    ): Response<EnhancedWorkoutResponse>

    // Overreaching Detection
    @GET("workout-execution/workouts/overreaching_status")
    suspend fun getOverreachingStatus(
        @Header("Authorization") token: String
    ): Response<OverreachingStatus>

    // Plan Adjustment
    @POST("workout-execution/workouts/adjust-plan-for-overreaching")
    suspend fun adjustPlanForOverreaching(
        @Header("Authorization") token: String
    ): Response<PlanAdjustmentResponse>

    // Training Statistics
    @GET("workout-execution/workouts/planned-vs-actual-stats")
    suspend fun getTrainingStats(
        @Header("Authorization") token: String,
        @Query("days") days: Int = 30
    ): Response<TrainingStats>
}
