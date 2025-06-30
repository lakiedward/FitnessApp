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
    val workout_type: String?,
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
                        "Ramp" -> WorkoutStep.Ramp(
                            duration = (stepMap["duration"] as Number).toInt(),
                            start_power = (stepMap["start_power"] as Number).toFloat(),
                            end_power = (stepMap["end_power"] as Number).toFloat()
                        )
                        "FreeRide" -> WorkoutStep.FreeRide(
                            duration = (stepMap["duration"] as Number).toInt(),
                            power_low = (stepMap["power_low"] as Number).toFloat(),
                            power_high = (stepMap["power_high"] as Number).toFloat()
                        )
                        "IntervalsP" -> WorkoutStep.IntervalsP(
                            repeat = (stepMap["repeat"] as Number).toInt(),
                            on_duration = (stepMap["on_duration"] as Number).toInt(),
                            on_power = (stepMap["on_power"] as Number).toFloat(),
                            off_duration = (stepMap["off_duration"] as Number).toInt(),
                            off_power = (stepMap["off_power"] as Number).toFloat(),
                            cadence = (stepMap["cadence"] as? Number)?.toInt()
                        )
                        "Pyramid" -> WorkoutStep.Pyramid(
                            repeat = (stepMap["repeat"] as Number).toInt(),
                            step_duration = (stepMap["step_duration"] as Number).toInt(),
                            start_power = (stepMap["start_power"] as Number).toFloat(),
                            peak_power = (stepMap["peak_power"] as Number).toFloat(),
                            end_power = (stepMap["end_power"] as Number).toFloat()
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
