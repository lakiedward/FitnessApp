package com.example.fitnessapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.api.StravaApiService
import com.example.fitnessapp.model.CyclingFtpResponse
import com.example.fitnessapp.model.FTPEstimate
import com.example.fitnessapp.model.RunningFtpResponse
import com.example.fitnessapp.model.SwimmingPaceResponse
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.model.WorkoutStep.FreeRide
import com.example.fitnessapp.model.WorkoutStep.IntervalsP
import com.example.fitnessapp.model.WorkoutStep.IntervalsT
import com.example.fitnessapp.model.WorkoutStep.Pyramid
import com.example.fitnessapp.model.WorkoutStep.Ramp
import com.example.fitnessapp.model.WorkoutStep.SteadyState
import kotlinx.coroutines.launch

class TrainingDetailViewModel : ViewModel() {
    private val stravaApiService = RetrofitClient.retrofit.create(StravaApiService::class.java)
    private val apiService = RetrofitClient.retrofit.create(ApiService::class.java)
    
    private val _ftpEstimate = MutableLiveData<FTPEstimate?>()
    val ftpEstimate: LiveData<FTPEstimate?> = _ftpEstimate

    private val _cyclingFtp = MutableLiveData<CyclingFtpResponse?>()
    val cyclingFtp: LiveData<CyclingFtpResponse?> = _cyclingFtp

    private val _runningFtp = MutableLiveData<RunningFtpResponse?>()
    val runningFtp: LiveData<RunningFtpResponse?> = _runningFtp

    private val _swimmingPace = MutableLiveData<SwimmingPaceResponse?>()
    val swimmingPace: LiveData<SwimmingPaceResponse?> = _swimmingPace

    private val _calculatedMetrics = MutableLiveData<TrainingMetrics>()
    val calculatedMetrics: LiveData<TrainingMetrics> = _calculatedMetrics

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Add a flag to track if data has been loaded
    private var dataLoaded = false

    fun ensureDataLoaded(token: String, workoutType: String) {
        if (!dataLoaded) {
            when (workoutType.lowercase()) {
                "running" -> fetchRunningFtp(token)
                "swimming" -> fetchSwimmingPace(token)
                "cycling" -> fetchCyclingFtp(token)
                else -> fetchCyclingFtp(token)
            }
            dataLoaded = true
        }
    }

    fun calculateTrainingMetrics(training: TrainingPlan, ftp: Float) {
        Log.d("TrainingDetailViewModel", "🚀 calculateTrainingMetrics CALLED!")
        Log.d(
            "TrainingDetailViewModel",
            "Training ID: ${training.id}, Name: ${training.workout_name}"
        )
        Log.d("TrainingDetailViewModel", "Training Type: ${training.workout_type}")
        Log.d("TrainingDetailViewModel", "Steps JSON: ${training.stepsJson}")
        Log.d("TrainingDetailViewModel", "FTP parameter: $ftp")
        Log.d(
            "TrainingDetailViewModel",
            "Running FTP from LiveData: ${_runningFtp.value?.runningFtp}"
        )

        val steps = training.steps
        if (steps == null) {
            Log.d("TrainingDetailViewModel", "ERROR: No steps found for training!")
            return
        }

        Log.d("TrainingDetailViewModel", "Steps count: ${steps.size}")

        var totalTSS = 0f
        var totalDuration = 0
        var totalCalories = 0f

        val workoutType = training.workout_type?.lowercase() ?: "cycling"
        Log.d("TrainingDetailViewModel", "Processing $workoutType workout")

        steps.forEachIndexed { index, step ->
            Log.d("TrainingDetailViewModel", "Processing step $index: ${step::class.simpleName}")
            when (step) {
                is SteadyState -> {
                    val duration = step.duration
                    val intensity = step.power

                    Log.d(
                        "TrainingDetailViewModel",
                        "SteadyState - Duration: ${duration}s, Intensity: ${intensity}%"
                    )

                    val tss = calculateTSSForWorkoutType(duration, intensity, ftp, workoutType)
                    val calories =
                        calculateCaloriesForWorkoutType(duration, intensity, ftp, workoutType)

                    Log.d("TrainingDetailViewModel", "Calculated - TSS: $tss, Calories: $calories")

                    totalTSS += tss
                    totalDuration += duration
                    totalCalories += calories
                }

                is IntervalsT -> {
                    Log.d(
                        "TrainingDetailViewModel",
                        "IntervalsT - Repeat: ${step.repeat}, On: ${step.on_duration}s@${step.on_power}%, Off: ${step.off_duration}s@${step.off_power}%"
                    )
                    repeat(step.repeat) {
                        val onTSS = calculateTSSForWorkoutType(
                            step.on_duration,
                            step.on_power,
                            ftp,
                            workoutType
                        )
                        val onCalories = calculateCaloriesForWorkoutType(
                            step.on_duration,
                            step.on_power,
                            ftp,
                            workoutType
                        )
                        val offTSS = calculateTSSForWorkoutType(
                            step.off_duration,
                            step.off_power,
                            ftp,
                            workoutType
                        )
                        val offCalories = calculateCaloriesForWorkoutType(
                            step.off_duration,
                            step.off_power,
                            ftp,
                            workoutType
                        )

                        totalTSS += onTSS + offTSS
                        totalDuration += step.on_duration + step.off_duration
                        totalCalories += onCalories + offCalories
                    }
                }

                is Ramp -> {
                    val duration = step.duration
                    val avgPower = (step.start_power + step.end_power) / 2f
                    val tss = calculateTSSForWorkoutType(duration, avgPower, ftp, workoutType)
                    val calories =
                        calculateCaloriesForWorkoutType(duration, avgPower, ftp, workoutType)
                    totalTSS += tss
                    totalDuration += duration
                    totalCalories += calories
                }

                is FreeRide -> {
                    val duration = step.duration
                    val avgPower = (step.power_low + step.power_high) / 2f
                    val tss = calculateTSSForWorkoutType(duration, avgPower, ftp, workoutType)
                    val calories =
                        calculateCaloriesForWorkoutType(duration, avgPower, ftp, workoutType)
                    totalTSS += tss
                    totalDuration += duration
                    totalCalories += calories
                }

                is IntervalsP -> {
                    repeat(step.repeat) {
                        val onTSS = calculateTSSForWorkoutType(
                            step.on_duration,
                            step.on_power,
                            ftp,
                            workoutType
                        )
                        val onCalories = calculateCaloriesForWorkoutType(
                            step.on_duration,
                            step.on_power,
                            ftp,
                            workoutType
                        )
                        val offTSS = calculateTSSForWorkoutType(
                            step.off_duration,
                            step.off_power,
                            ftp,
                            workoutType
                        )
                        val offCalories = calculateCaloriesForWorkoutType(
                            step.off_duration,
                            step.off_power,
                            ftp,
                            workoutType
                        )
                        totalTSS += onTSS + offTSS
                        totalDuration += step.on_duration + step.off_duration
                        totalCalories += onCalories + offCalories
                    }
                }

                is Pyramid -> {
                    repeat(step.repeat) {
                        val upTSS = calculateTSSForWorkoutType(
                            step.step_duration,
                            step.peak_power,
                            ftp,
                            workoutType
                        )
                        val upCalories = calculateCaloriesForWorkoutType(
                            step.step_duration,
                            step.peak_power,
                            ftp,
                            workoutType
                        )
                        val downTSS = calculateTSSForWorkoutType(
                            step.step_duration,
                            step.end_power,
                            ftp,
                            workoutType
                        )
                        val downCalories = calculateCaloriesForWorkoutType(
                            step.step_duration,
                            step.end_power,
                            ftp,
                            workoutType
                        )
                        totalTSS += upTSS + downTSS
                        totalDuration += step.step_duration * 2
                        totalCalories += upCalories + downCalories
                    }
                }
            }
        }

        Log.d(
            "TrainingDetailViewModel",
            "Final metrics - TSS: $totalTSS, Duration: $totalDuration, Calories: $totalCalories"
        )

        _calculatedMetrics.value = TrainingMetrics(
            training = training,
            tss = totalTSS,
            duration = totalDuration,
            calories = totalCalories
        )
    }

    fun fetchFTPEstimate(token: String, days: Int = 30) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = stravaApiService.estimateFtp("Bearer $token", days)
                _ftpEstimate.value = response
                // Calculate metrics if we have a training plan
                _calculatedMetrics.value?.let { metrics ->
                    calculateTrainingMetrics(metrics.training, response.estimatedFTP)
                }
            } catch (e: Exception) {
                _error.value = "Failed to fetch FTP estimate"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchLastFtpEstimateFromDb(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = stravaApiService.getLastFtpEstimateFromDb("Bearer $token")
                _ftpEstimate.value = response
                Log.d("TrainingDetailViewModel", "FTP estimate fetched from DB: ${response.estimatedFTP} watts")
            } catch (e: Exception) {
                Log.d("TrainingDetailViewModel", "No FTP estimate found in DB, will use default: ${e.message}")
                _error.value = "No FTP estimate found in database"
                _ftpEstimate.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchRunningFtp(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = apiService.getRunningFtp("Bearer $token")
                if (response.isSuccessful) {
                    _runningFtp.value = response.body()
                    Log.d(
                        "TrainingDetailViewModel",
                        "Running FTP fetched: ${response.body()?.runningFtp}"
                    )
                } else {
                    _error.value = "Failed to fetch running FTP"
                }
            } catch (e: Exception) {
                Log.d("TrainingDetailViewModel", "Error fetching running FTP: ${e.message}")
                _error.value = "Error fetching running FTP"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchSwimmingPace(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = apiService.getSwimmingPace("Bearer $token")
                if (response.isSuccessful) {
                    _swimmingPace.value = response.body()
                    Log.d(
                        "TrainingDetailViewModel",
                        "Swimming pace fetched: ${response.body()?.pace100m}"
                    )
                } else {
                    _error.value = "Failed to fetch swimming pace"
                }
            } catch (e: Exception) {
                Log.d("TrainingDetailViewModel", "Error fetching swimming pace: ${e.message}")
                _error.value = "Error fetching swimming pace"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchCyclingFtp(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = apiService.getCyclingFtp("Bearer $token")
                if (response.isSuccessful) {
                    _cyclingFtp.value = response.body()
                    Log.d(
                        "TrainingDetailViewModel",
                        "Cycling FTP fetched: ${response.body()?.cyclingFtp}"
                    )
                } else {
                    _error.value = "Failed to fetch cycling FTP"
                }
            } catch (e: Exception) {
                Log.d("TrainingDetailViewModel", "Error fetching cycling FTP: ${e.message}")
                _error.value = "Error fetching cycling FTP"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun calculateTSSForWorkoutType(
        duration: Int,
        intensity: Float,
        ftp: Float,
        workoutType: String
    ): Float {
        return when (workoutType) {
            "cycling" -> calculateCyclingTSS(duration, intensity, ftp)
            "running" -> calculateRunningTSS(duration, intensity)
            "swimming" -> calculateSwimmingTSS(duration, intensity)
            else -> calculateCyclingTSS(duration, intensity, ftp) // Default to cycling
        }
    }

    private fun calculateCaloriesForWorkoutType(
        duration: Int,
        intensity: Float,
        ftp: Float,
        workoutType: String
    ): Float {
        return when (workoutType) {
            "cycling" -> calculateCyclingCalories(duration, intensity, ftp)
            "running" -> calculateRunningCalories(duration, intensity)
            "swimming" -> calculateSwimmingCalories(duration, intensity)
            else -> calculateCyclingCalories(duration, intensity, ftp) // Default to cycling
        }
    }

    // Cycling calculations (updated)
    private fun calculateCyclingTSS(duration: Int, intensity: Float, ftp: Float): Float {
        val durationHours = duration / 3600f
        val intensityFactor = intensity

        // Use the new cycling FTP if available, otherwise use the passed FTP parameter
        val cyclingFtpValue = _cyclingFtp.value?.cyclingFtp?.toFloat() ?: ftp
        val tss = durationHours * (intensityFactor * intensityFactor) * 100

        Log.d(
            "TrainingDetailViewModel",
            "Cycling TSS - Duration: ${duration}s, Intensity: ${intensity}%, FTP: ${cyclingFtpValue}w, TSS: ${tss}"
        )
        
        return tss
    }

    private fun calculateCyclingCalories(duration: Int, intensity: Float, ftp: Float): Float {
        val durationHours = duration / 3600f

        // Use the new cycling FTP if available, otherwise use the passed FTP parameter
        val cyclingFtpValue = _cyclingFtp.value?.cyclingFtp?.toFloat() ?: ftp
        val powerWatts = intensity * cyclingFtpValue
        return powerWatts * durationHours * 3.6f
    }

    // Running calculations
    private fun calculateRunningTSS(duration: Int, intensity: Float): Float {
        val durationHours = duration / 3600f
        val runningFtpValue = _runningFtp.value?.runningFtp ?: 4.0f

        // Intensity is already in decimal format (0.6 = 60%, 1.2 = 120%)
        // For running, if intensity is 0 or very small, use a default moderate intensity
        val effectiveIntensity = if (intensity <= 0.01f) {
            0.65f // Default to moderate intensity (65% effort as 0.65)
        } else {
            intensity
        }

        // No need to divide by 100 since intensity is already a factor
        val paceFactor = effectiveIntensity
        val tss = durationHours * (paceFactor * paceFactor) * 100

        Log.d("TrainingDetailViewModel", "=== RUNNING TSS CALCULATION DEBUG ===")
        Log.d(
            "TrainingDetailViewModel",
            "Running FTP from LiveData: ${_runningFtp.value?.runningFtp}"
        )
        Log.d("TrainingDetailViewModel", "Duration (seconds): $duration")
        Log.d("TrainingDetailViewModel", "Duration (hours): $durationHours")
        Log.d("TrainingDetailViewModel", "Original Intensity (factor): $intensity")
        Log.d("TrainingDetailViewModel", "Effective Intensity (factor): $effectiveIntensity")
        Log.d("TrainingDetailViewModel", "Pace Factor: $paceFactor")
        Log.d(
            "TrainingDetailViewModel",
            "TSS Formula: $durationHours * ($paceFactor * $paceFactor) * 100"
        )
        Log.d("TrainingDetailViewModel", "Final TSS: $tss")
        Log.d("TrainingDetailViewModel", "========================================")

        Log.d(
            "TrainingDetailViewModel",
            "Running TSS - Duration: ${duration}s, Intensity: ${intensity}%, FTP: ${runningFtpValue}m/s, TSS: ${tss}"
        )

        return tss
    }

    private fun calculateRunningCalories(duration: Int, intensity: Float): Float {
        val durationMinutes = duration / 60f
        val baseCaloriesPerMinute = 12f

        // Use effective intensity for calories too (intensity is already a factor)
        val effectiveIntensity = if (intensity <= 0.01f) 0.65f else intensity
        val intensityMultiplier = effectiveIntensity

        val calories = durationMinutes * baseCaloriesPerMinute * (0.5f + intensityMultiplier)

        Log.d(
            "TrainingDetailViewModel",
            "Running Calories - Duration: ${durationMinutes}min, Effective Intensity: $effectiveIntensity (factor), Calories: $calories"
        )

        return calories
    }

    // Swimming calculations
    private fun calculateSwimmingTSS(duration: Int, intensity: Float): Float {
        val durationHours = duration / 3600f
        val swimmingPaceValue = _swimmingPace.value?.pace100m ?: "1:30" // Default pace

        // Intensity is already in decimal format (0.7 = 70%, 1.0 = 100%)
        // For swimming, if intensity is 0 or very small, use a default moderate intensity
        val effectiveIntensity = if (intensity <= 0.01f) {
            0.70f // Default to moderate-high intensity (70% effort as 0.70)
        } else {
            intensity
        }

        // No need to divide by 100 since intensity is already a factor
        val intensityFactor = effectiveIntensity
        val tss = durationHours * (intensityFactor * intensityFactor) * 100

        Log.d("TrainingDetailViewModel", "=== SWIMMING TSS CALCULATION DEBUG ===")
        Log.d("TrainingDetailViewModel", "Duration (seconds): $duration")
        Log.d("TrainingDetailViewModel", "Duration (hours): $durationHours")
        Log.d("TrainingDetailViewModel", "Original Intensity (factor): $intensity")
        Log.d("TrainingDetailViewModel", "Effective Intensity (factor): $effectiveIntensity")
        Log.d("TrainingDetailViewModel", "Intensity Factor: $intensityFactor")
        Log.d("TrainingDetailViewModel", "Swimming Pace: $swimmingPaceValue")
        Log.d(
            "TrainingDetailViewModel",
            "TSS Formula: $durationHours * ($intensityFactor * $intensityFactor) * 100"
        )
        Log.d("TrainingDetailViewModel", "Final TSS: $tss")
        Log.d("TrainingDetailViewModel", "==========================================")

        Log.d(
            "TrainingDetailViewModel",
            "Swimming TSS - Duration: ${duration}s, Intensity: ${intensity}%, Pace: ${swimmingPaceValue}, TSS: ${tss}"
        )

        return tss
    }

    private fun calculateSwimmingCalories(duration: Int, intensity: Float): Float {
        val durationMinutes = duration / 60f
        val baseCaloriesPerMinute = 10f

        // Use effective intensity for calories too (intensity is already a factor)
        val effectiveIntensity = if (intensity <= 0.01f) 0.70f else intensity
        val intensityMultiplier = effectiveIntensity

        val calories = durationMinutes * baseCaloriesPerMinute * (0.5f + intensityMultiplier)

        Log.d(
            "TrainingDetailViewModel",
            "Swimming Calories - Duration: ${durationMinutes}min, Effective Intensity: $effectiveIntensity (factor), Calories: $calories"
        )

        return calories
    }

    // Legacy methods for backward compatibility
    private fun calculateTSS(duration: Int, intensity: Float, ftp: Float): Float {
        return calculateCyclingTSS(duration, intensity, ftp)
    }

    private fun calculateCalories(duration: Int, intensity: Float, ftp: Float): Float {
        return calculateCyclingCalories(duration, intensity, ftp)
    }

    fun clearError() {
        _error.value = null
    }
}

data class TrainingMetrics(
    val training: TrainingPlan,
    val tss: Float,
    val duration: Int,
    val calories: Float
)
