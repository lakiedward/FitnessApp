package com.example.fitnessapp.model

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken


data class TrainingPlanResponse(
    val message: String,
    val data: List<TrainingPlan>
)


data class TrainingPlan(
    val id: Int,
    val user_id: Int,
    val date: String,
    val workout_name: String,
    val duration: String,
    val intensity: String,
    val description: String,
    val zwo_path: String? = null,

    @SerializedName("steps_json")
    val stepsJson: String? = null
) {
    val steps: List<WorkoutStep>?
        get() = stepsJson?.let {
            try {
                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                Log.d("TrainingPlanDebug", "steps_json = $stepsJson")
                val rawList: List<Map<String, Any>> = Gson().fromJson(it, listType)

                rawList.mapNotNull { stepMap ->
                    when (stepMap["type"]) {
                        "SteadyState" -> WorkoutStep.SteadyState(
                            duration = (stepMap["duration"] as Number).toInt(),
                            power = (stepMap["power"] as Number).toFloat()
                        )
                        "IntervalsT" -> WorkoutStep.IntervalsT(
                            repeat = (stepMap["repeat"] as Number).toInt(),
                            on_duration = (stepMap["on_duration"] as Number).toInt(),
                            on_power = (stepMap["on_power"] as Number).toFloat(),
                            off_duration = (stepMap["off_duration"] as Number).toInt(),
                            off_power = (stepMap["off_power"] as Number).toFloat()
                        )
                        else -> null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
