package com.example.fitnessapp.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class StravaTokenResponse(
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_at")
    val expiresAt: Long,
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("athlete")
    val athlete: StravaAthlete
)

data class StravaAthlete(
    val id: Long,
    val username: String?,
    @SerializedName("firstname")
    val firstName: String,
    @SerializedName("lastname")
    val lastName: String,
    val city: String?,
    val state: String?,
    val country: String?,
    val sex: String?,
    val premium: Boolean,
    val summit: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("badge_type_id")
    val badgeTypeId: Int,
    val profile: String?,
    @SerializedName("profile_medium")
    val profileMedium: String?
)

data class StravaToken(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_at")
    val expiresAt: Long,
    @SerializedName("token_type")
    val tokenType: String = "Bearer"
)

data class StravaActivity(
    val id: Long,
    val name: String,
    val distance: Float?,
    @SerializedName("moving_time")
    val movingTime: Int,
    @SerializedName("elapsed_time")
    val elapsedTime: Int,
    val type: String,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("start_date_local")
    val startDateLocal: String,
    val timezone: String,
    val utc_offset: Float,
    val location_city: String?,
    val location_state: String?,
    val location_country: String?,
    @SerializedName("achievement_count")
    val achievementCount: Int,
    @SerializedName("kudos_count")
    val kudosCount: Int,
    @SerializedName("comment_count")
    val commentCount: Int,
    @SerializedName("athlete_count")
    val athleteCount: Int,
    @SerializedName("photo_count")
    val photoCount: Int,
    val map: StravaMap?,
    val trainer: Boolean,
    val commute: Boolean,
    val manual: Boolean,
    val private: Boolean,
    val flagged: Boolean,
    @SerializedName("workout_type")
    val workoutType: Int?,
    @SerializedName("upload_id")
    val uploadId: Long?,
    @SerializedName("external_id")
    val externalId: String?,
    @SerializedName("average_speed")
    val averageSpeed: Float?,
    @SerializedName("max_speed")
    val maxSpeed: Float?,
    @SerializedName("has_heartrate")
    val hasHeartrate: Boolean,
    @SerializedName("average_heartrate")
    val averageHeartrate: Float?,
    @SerializedName("max_heartrate")
    val maxHeartrate: Int?,
    @SerializedName("elev_high")
    val elevationHigh: Float?,
    @SerializedName("elev_low")
    val elevationLow: Float?,
    @SerializedName("total_elevation_gain")
    val totalElevationGain: Float?,
    @SerializedName("start_latlng")
    val startLatLng: List<Float>?,
    @SerializedName("end_latlng")
    val endLatLng: List<Float>?,
    val achievements: List<StravaAchievement>? = null,
    @SerializedName("segment_efforts")
    val segmentEfforts: List<StravaSegmentEffort>? = null,
    @SerializedName("HrTSS")
    val hrTss: Float? = null
)

data class StravaMap(
    val id: String,
    val polyline: String?,
    @SerializedName("summary_polyline")
    val summaryPolyline: String?
)

data class StravaAchievement(
    val type: String,
    val type_id: Int,
    val rank: Int
)

data class StravaSegmentEffort(
    val id: Long,
    val name: String,
    val activity: StravaActivity?,
    val athlete: StravaAthlete?,
    @SerializedName("elapsed_time")
    val elapsedTime: Int,
    @SerializedName("moving_time")
    val movingTime: Int,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("start_date_local")
    val startDateLocal: String,
    val distance: Float,
    @SerializedName("start_index")
    val startIndex: Int,
    @SerializedName("end_index")
    val endIndex: Int,
    @SerializedName("average_cadence")
    val averageCadence: Float?,
    @SerializedName("average_watts")
    val averageWatts: Float?,
    @SerializedName("device_watts")
    val deviceWatts: Boolean?,
    @SerializedName("average_heartrate")
    val averageHeartrate: Float?,
    @SerializedName("max_heartrate")
    val maxHeartrate: Int?,
    val segment: StravaSegment?,
    @SerializedName("kom_rank")
    val komRank: Int?,
    @SerializedName("pr_rank")
    val prRank: Int?,
    val hidden: Boolean
)

data class StravaSegment(
    val id: Long,
    val name: String,
    val activity_type: String,
    val distance: Float,
    @SerializedName("average_grade")
    val averageGrade: Float,
    @SerializedName("maximum_grade")
    val maximumGrade: Float,
    @SerializedName("elevation_high")
    val elevationHigh: Float,
    @SerializedName("elevation_low")
    val elevationLow: Float,
    @SerializedName("start_latlng")
    val startLatLng: List<Float>,
    @SerializedName("end_latlng")
    val endLatLng: List<Float>,
    @SerializedName("climb_category")
    val climbCategory: Int,
    val city: String?,
    val state: String?,
    val country: String?,
    val private: Boolean,
    @SerializedName("athlete_count")
    val athleteCount: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("total_elevation_gain")
    val totalElevationGain: Float,
    val map: StravaMap?,
    @SerializedName("effort_count")
    val effortCount: Int,
    @SerializedName("athlete_segment_stats")
    val athleteSegmentStats: StravaAthleteSegmentStats?
)

data class StravaAthleteSegmentStats(
    @SerializedName("pr_elapsed_time")
    val prElapsedTime: Int?,
    @SerializedName("pr_date")
    val prDate: String?,
    @SerializedName("effort_count")
    val effortCount: Int
)

data class FTPEstimate(
    @SerializedName("date")
    val date: String,
    @SerializedName("estimated_ftp")
    val estimatedFTP: Float,
    @SerializedName("confidence")
    val confidence: Float,  // 0-1 scale
    @SerializedName("source_activities")
    val sourceActivities: List<Long>,  // List of activity IDs
    @SerializedName("method")
    val method: String,  // e.g., "fthr_based+ftp_estimators"
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("confidence_metrics")
    val confidenceMetrics: Map<String, Any>? = null,
    @SerializedName("fthr_value")
    val fthrValue: Float? = null,
    @SerializedName("fthr_source")
    val fthrSource: String? = null,
    @SerializedName("fthr_age_days")
    val fthrAgeDays: Int? = null,
    @SerializedName("weekly_ftp")
    val weeklyFTP: List<WeeklyFTP>? = null
)

data class WeeklyFTP(
    @SerializedName("week")
    val week: String,
    @SerializedName("week_end")
    val weekEnd: String,
    @SerializedName("ftp_20min_est")
    val ftp20minEst: Float,
    @SerializedName("ftp_hr_est")
    val ftpHrEst: Float,
    @SerializedName("ftp_tss_est")
    val ftpTssEst: Float,
    @SerializedName("ftp_final")
    val ftpFinal: Float,
    @SerializedName("ftp_final_wkg")
    val ftpFinalWkg: Float,
    @SerializedName("categorie")
    val categorie: String,
    @SerializedName("tss_week")
    val tssWeek: Float,
    @SerializedName("stare_saptamana")
    val stareSaptamana: String
)

data class StravaUserData(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("strava_id")
    val stravaId: Long,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_expires_at")
    val tokenExpiresAt: Long,
    val lastActivitySync: Date? = null,
    val lastFTPUpdate: Date? = null,
    val currentFTP: Float? = null,
    val ftpHistory: List<FTPEstimate> = emptyList()
)

data class PowerHeartRateMetrics(
    val activityId: Long,
    val date: Date,
    val avgPower: Float,
    val avgHeartRate: Float,
    val efficiencyFactor: Float,  // Power/HR ratio
    val normalizedPower: Float? = null,
    val intensityFactor: Float? = null,  // NP/FTP ratio
    val trainingStressScore: Float? = null,  // TSS
    val decoupling: Float? = null,  // % difference between first and second half
    val powerHeartRateRatio: Float,  // watts per bpm
    val isSteadyState: Boolean,
    val durationMinutes: Float,
    val notes: String? = null
)

data class EfficiencyTrend(
    val userId: String,
    val date: Date,
    val efficiencyFactor: Float,
    val powerHeartRateRatio: Float,
    val confidence: Float,
    val sourceActivities: List<Long>,
    val trendDirection: String,  // "improving", "declining", "stable"
    val trendStrength: Float,  // 0-1 scale
    val suggestedFTPChange: Float? = null,
    val notes: String? = null
)

data class ActivityStreamsResponse(
    @SerializedName("activity_id")
    val activityId: Long,
    @SerializedName("activity_name")
    val activityName: String,
    @SerializedName("activity_type")
    val activityType: String,
    val source: String, // "database" or "strava_api"
    val streams: Map<String, StreamData>,
    @SerializedName("available_streams")
    val availableStreams: List<String>
)

data class StreamData(
    val data: List<Float>,
    @SerializedName("data_length")
    val dataLength: Int,
    val resolution: Int
)
