package com.example.fitnessapp.viewmodel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.bluetooth.BleTrainerService
import com.example.fitnessapp.model.*
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.api.RetrofitClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException
import android.util.Log
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

class TrainerViewModel(private val context: Context) : ViewModel() {
    private val bleTrainerService = BleTrainerService(context)
    private val apiService = RetrofitClient.retrofit.create(ApiService::class.java)

    // Support for multiple concurrent connections
    private val connectionJobs = mutableMapOf<String, Job>()
    private val _connectedDevices = MutableLiveData<List<TrainerDevice>>()
    val connectedDevices: LiveData<List<TrainerDevice>> = _connectedDevices

    private val _currentSession = MutableLiveData<WorkoutSession?>()
    val currentSession: LiveData<WorkoutSession?> = _currentSession

    private val _realTimeData = MutableLiveData<RealTimeData>()
    val realTimeData: LiveData<RealTimeData> = _realTimeData

    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _availableDevices = MutableLiveData<List<TrainerDevice>>()
    val availableDevices: LiveData<List<TrainerDevice>> = _availableDevices

    // ERG control variables
    private var currentErgJob: Job? = null
    private var currentTargetPower: Int = 0
    private val _ergMode = MutableLiveData<Boolean>()
    val ergMode: LiveData<Boolean> = _ergMode

    // FTP integration
    private val _cyclingFtp = MutableLiveData<CyclingFtpResponse?>()
    val cyclingFtp: LiveData<CyclingFtpResponse?> = _cyclingFtp
    private var userFtp: Int = 250 // Default FTP, will be updated from API

    // Power zones based on FTP
    private val _currentPowerZone = MutableLiveData<PowerZone>()
    val currentPowerZone: LiveData<PowerZone> = _currentPowerZone

    companion object {
        private const val TAG = "TrainerViewModel"
        private const val DEFAULT_FTP = 250 // Default FTP if API fails
    }

    init {
        _connectionState.value = ConnectionState.DISCONNECTED
        _realTimeData.value = RealTimeData()
        _ergMode.value = false
        _currentPowerZone.value = PowerZone.RECOVERY
    }

    /**
     * Scanează pentru dispozitive BLE trainer reale
     */
    fun scanForDevices() {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.SCANNING
                _availableDevices.value = emptyList()

                Log.d(TAG, "=== Starting BLE scan for trainer devices ===")

                // Verifică dacă Bluetooth e activat
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    Log.e(TAG, "Bluetooth not available or not enabled")
                    _connectionState.value = ConnectionState.ERROR
                    return@launch
                }

                val foundDevices = mutableListOf<TrainerDevice>()
                var scanJob: kotlinx.coroutines.Job? = null

                scanJob = launch {
                    bleTrainerService.scanForTrainers()
                        .catch { exception ->
                            Log.e(TAG, "Error during BLE scan", exception)
                            _connectionState.value = ConnectionState.ERROR
                        }
                        .collect { device ->
                            if (!foundDevices.any { it.id == device.id }) {
                                foundDevices.add(device)
                                _availableDevices.value = foundDevices.toList()
                                Log.d(TAG, "Found device: ${device.name} (${device.type}) - RSSI: ${device.signalStrength}")
                            }
                        }
                }

                // Timeout după 15 secunde
                delay(15000)

                scanJob?.cancel()

                Log.d(TAG, "=== BLE scan completed. Found ${foundDevices.size} devices ===")
                foundDevices.forEach { device ->
                    Log.d(TAG, "  - ${device.name} (${device.id}) - ${device.type}")
                }

                _connectionState.value = ConnectionState.DISCONNECTED

            } catch (e: Exception) {
                Log.e(TAG, "Error in scanForDevices", e)
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    /**
     * Conectează la un dispozitiv trainer real
     */
    fun connectToDevice(device: TrainerDevice) {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                Log.d(TAG, "Attempting to connect to ${device.name} (${device.id})")

                // Găsește dispozitivul Bluetooth
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.id)

                if (bluetoothDevice != null) {
                    // Timeout pentru conexiune (30 secunde)
                    val connectionJob = launch {
                        var isConnected = false

                        bleTrainerService.connectToTrainer(bluetoothDevice)
                            .catch { exception ->
                                Log.e(TAG, "Error connecting to ${device.name}", exception)
                                _connectionState.value = ConnectionState.ERROR
                            }
                            .collect { newData ->
                                // Prima dată când primim date = conexiunea e stabilită
                                if (!isConnected) {
                                    isConnected = true
                                    // ADAUGĂ la listă, nu înlocui!
                                    val currentList = _connectedDevices.value?.toMutableList() ?: mutableListOf()
                                    if (!currentList.any { it.id == device.id }) {
                                        currentList.add(device.copy(isConnected = true))
                                        _connectedDevices.value = currentList
                                    }
                                    _connectionState.value = ConnectionState.CONNECTED
                                    Log.d(TAG, "Successfully connected to ${device.name}. Total connected: ${currentList.size}")
                                }

                                // Combină datele din multiple dispozitive
                                val currentData = _realTimeData.value ?: RealTimeData()
                                val updatedData = when (device.type) {
                                    TrainerType.HEART_RATE_MONITOR -> currentData.copy(heartRate = newData.heartRate)
                                    else -> currentData.copy(power = newData.power, cadence = newData.cadence) // Pentru trainers
                                }
                                _realTimeData.value = updatedData
                                Log.d(TAG, "Updated data: Power=${updatedData.power}W, HR=${updatedData.heartRate}bpm")
                            }
                    }

                    connectionJobs[device.id] = connectionJob

                    // Timeout după 30 secunde
                    delay(30000)

                    if (_connectionState.value == ConnectionState.CONNECTING) {
                        Log.e(TAG, "Connection timeout for ${device.name}")
                        connectionJob.cancel()
                        connectionJobs.remove(device.id)
                        _connectionState.value = ConnectionState.ERROR
                    }

                } else {
                    Log.e(TAG, "Bluetooth device not found: ${device.id}")
                    _connectionState.value = ConnectionState.ERROR
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Connection job cancelled gracefully: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error in connectToDevice", e)
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    /**
     * Deconectează de la dispozitivul trainer
     */
    fun disconnectDevice(deviceId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Disconnecting from device: $deviceId")

                // Deconectează serviciul BLE pentru dispozitivul specific
                bleTrainerService.disconnect(deviceId)
                connectionJobs[deviceId]?.cancel()
                connectionJobs.remove(deviceId)

                // Actualizează starea
                val currentDevices = _connectedDevices.value ?: emptyList()
                _connectedDevices.value = currentDevices.filter { it.id != deviceId }

                if (_connectedDevices.value?.isEmpty() == true) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }

                Log.d(TAG, "Successfully disconnected from device: $deviceId. Remaining: ${_connectedDevices.value?.size ?: 0}")

            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting device", e)
            }
        }
    }

    /**
     * Obține FTP-ul utilizatorului din API
     */
    fun fetchUserFtp(token: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching user FTP from API...")
                val response = apiService.getCyclingFtp("Bearer $token")

                if (response.isSuccessful) {
                    val ftpResponse = response.body()
                    if (ftpResponse != null) {
                        _cyclingFtp.value = ftpResponse
                        userFtp = ftpResponse.cyclingFtp
                        Log.d(TAG, "✓ FTP fetched successfully: ${userFtp}W (FTHR: ${ftpResponse.fthrCycling}bpm)")
                    } else {
                        Log.w(TAG, "FTP response body is null, using default FTP: ${DEFAULT_FTP}W")
                        userFtp = DEFAULT_FTP
                    }
                } else {
                    Log.e(TAG, "Failed to fetch FTP: ${response.code()} - ${response.message()}")
                    userFtp = DEFAULT_FTP
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching FTP", e)
                userFtp = DEFAULT_FTP
            }
        }
    }

    fun startWorkout(trainingPlan: TrainingPlan, userToken: String? = null) {
        Log.d(TAG, "[DEBUG_LOG] Starting workout with trainingPlan ID: ${trainingPlan.id}, name: ${trainingPlan.workout_name}")

        // Obține FTP-ul înainte de a începe antrenamentul
        userToken?.let { token ->
            fetchUserFtp(token)
        }

        val session = WorkoutSession(
            trainingPlan = trainingPlan,
            startTime = System.currentTimeMillis(),
            totalSteps = trainingPlan.steps?.size ?: 0
        )
        _currentSession.value = session.copy(isActive = true)

        // Începe monitorizarea timpului scurs
        startElapsedTimeTracking()

        // Setează puterea țintă pentru primul pas (după ce FTP-ul e obținut)
        viewModelScope.launch {
            delay(1000) // Așteaptă să se obțină FTP-ul
            setCurrentStepPower()
        }

        Log.d(TAG, "✓ Started workout: ${trainingPlan.workout_name} with FTP: ${userFtp}W")
    }

    fun pauseWorkout() {
        _currentSession.value = _currentSession.value?.copy(isPaused = true)
        // Setează puterea la 0 când se pune pauză
        setTrainerResistance(0f)
        // Oprește job-ul ERG curent
        currentErgJob?.cancel()
        Log.d(TAG, "Workout paused - ERG set to 0W")
    }

    fun resumeWorkout() {
        _currentSession.value = _currentSession.value?.copy(isPaused = false)
        // Restabilește puterea pentru pasul curent
        setCurrentStepPower()
        Log.d(TAG, "Workout resumed - ERG power restored")
    }

    fun stopWorkout(onMessage: (String) -> Unit, onSaveCompleted: () -> Unit) {
        val session = _currentSession.value
        if (session != null) {
            // Salvează workout-ul în backend înainte de a-l șterge
            saveWorkoutToBackend(session, onMessage, onSaveCompleted)
        } else {
            onSaveCompleted() // Continuă navigarea chiar dacă nu există sesiune
        }

        _currentSession.value = null
        // Oprește ERG control și setează puterea la 0
        currentErgJob?.cancel()
        setTrainerResistance(0f)
        _ergMode.value = false
        Log.d(TAG, "Workout stopped - ERG disabled")
    }

    fun skipToNextStep() {
        val session = _currentSession.value
        if (session != null && session.currentStep < session.totalSteps - 1) {
            _currentSession.value = session.copy(currentStep = session.currentStep + 1)
            // Setează puterea pentru noul pas
            setCurrentStepPower()
            Log.d(TAG, "Skipped to step ${session.currentStep + 2}/${session.totalSteps}")
        }
    }

    /**
     * Salvează workout-ul în backend după finalizare
     */
    private fun saveWorkoutToBackend(
        session: WorkoutSession, 
        onMessage: (String) -> Unit, 
        onSaveCompleted: () -> Unit
    ) {
        viewModelScope.launch(NonCancellable) { // Folosește NonCancellable pentru operații critice
            try {
                // Obține token-ul de autentificare din SharedPreferences
                val sharedPreferences = context.getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("jwt_token", null)

                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "JWT token not found, cannot save workout")
                    onMessage("Eroare: Token de autentificare lipsă")
                    onSaveCompleted() // Continuă navigarea chiar dacă nu salvezi
                    return@launch
                }

                // Show saving message
                onMessage("Salvare workout...")

                // Calculează metrici de performanță din datele în timp real
                val currentRealTimeData = _realTimeData.value ?: RealTimeData()
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val endTime = System.currentTimeMillis()

                // Convertește WorkoutStep-urile în WorkoutStepData pentru API
                val workoutStepsData = session.trainingPlan.steps?.mapIndexed { index, step ->
                    WorkoutStepData(
                        stepNumber = index + 1,
                        duration = when (step) {
                            is WorkoutStep.SteadyState -> step.duration
                            is WorkoutStep.IntervalsT -> step.on_duration + step.off_duration
                            is WorkoutStep.Ramp -> step.duration
                            is WorkoutStep.FreeRide -> step.duration
                            is WorkoutStep.IntervalsP -> step.on_duration + step.off_duration
                            is WorkoutStep.Pyramid -> step.step_duration
                        },
                        targetPower = when (step) {
                            is WorkoutStep.SteadyState -> convertToAbsolutePower(step.power)
                            is WorkoutStep.IntervalsT -> convertToAbsolutePower(step.on_power)
                            is WorkoutStep.Ramp -> convertToAbsolutePower(step.start_power)
                            is WorkoutStep.FreeRide -> convertToAbsolutePower(step.power_low)
                            is WorkoutStep.IntervalsP -> convertToAbsolutePower(step.on_power)
                            is WorkoutStep.Pyramid -> convertToAbsolutePower(step.start_power)
                        },
                        targetHeartRate = null, // Nu avem target HR în WorkoutStep
                        targetCadence = when (step) {
                            is WorkoutStep.IntervalsP -> step.cadence
                            else -> null
                        },
                        actualPower = currentRealTimeData.power,
                        actualHeartRate = if (currentRealTimeData.heartRate > 0) currentRealTimeData.heartRate else null,
                        actualCadence = if (currentRealTimeData.cadence > 0) currentRealTimeData.cadence else null,
                        stepType = when (step) {
                            is WorkoutStep.SteadyState -> "steady_state"
                            is WorkoutStep.IntervalsT -> "intervals_time"
                            is WorkoutStep.Ramp -> "ramp"
                            is WorkoutStep.FreeRide -> "free_ride"
                            is WorkoutStep.IntervalsP -> "intervals_power"
                            is WorkoutStep.Pyramid -> "pyramid"
                        },
                        description = when (step) {
                            is WorkoutStep.SteadyState -> "Steady state at ${step.power} power"
                            is WorkoutStep.IntervalsT -> "Intervals: ${step.repeat}x ${step.on_duration}s on/${step.off_duration}s off"
                            is WorkoutStep.Ramp -> "Ramp from ${step.start_power} to ${step.end_power}"
                            is WorkoutStep.FreeRide -> "Free ride ${step.power_low}-${step.power_high}"
                            is WorkoutStep.IntervalsP -> "Power intervals: ${step.repeat}x ${step.on_duration}s"
                            is WorkoutStep.Pyramid -> "Pyramid: ${step.repeat} steps"
                        }
                    )
                }

                // Creează obiectul EnhancedWorkoutRequest
                Log.d(TAG, "[DEBUG_LOG] Creating EnhancedWorkoutRequest with plannedWorkoutId: ${session.trainingPlan.id}")
                val enhancedWorkoutRequest = EnhancedWorkoutRequest(
                    plannedWorkoutId = session.trainingPlan.id,  // Folosește planned_workout_id
                    isPlanned = true,  // Marchează că e un workout planificat
                    workoutName = session.trainingPlan.workout_name,
                    startTime = sdf.format(Date(session.startTime)),
                    endTime = sdf.format(Date(endTime)),
                    duration = session.elapsedTime,
                    averagePower = currentRealTimeData.power,
                    maxPower = currentRealTimeData.power,
                    averageHeartRate = if (currentRealTimeData.heartRate > 0) currentRealTimeData.heartRate else null,
                    maxHeartRate = if (currentRealTimeData.heartRate > 0) currentRealTimeData.heartRate else null,
                    distance = if (currentRealTimeData.distance > 0) currentRealTimeData.distance else null,
                    caloriesBurned = null,
                    workoutType = "cycling",
                    completed = true,
                    notes = "Workout completat din aplicație"
                )

                // Apelează API-ul pentru salvare cu noul endpoint
                Log.d(TAG, "[DEBUG_LOG] Sending workout to API with plannedWorkoutId: ${enhancedWorkoutRequest.plannedWorkoutId}")
                Log.d(TAG, "[DEBUG_LOG] Full EnhancedWorkoutRequest object: $enhancedWorkoutRequest")
                val response = apiService.saveEnhancedWorkout("Bearer $token", enhancedWorkoutRequest)

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "[DEBUG_LOG] Enhanced workout saved successfully: ${result?.message} (ID: ${result?.workoutId})")
                    Log.d(TAG, "[DEBUG_LOG] Response workout details: ${result?.workout}")
                    onMessage("Workout salvat cu succes!")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "[DEBUG_LOG] Failed to save enhanced workout: ${response.code()} - $errorBody")
                    onMessage("Eroare la salvarea workout-ului: ${response.code()}")
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Save job cancelled gracefully: ${e.message}")
                // Nu este o eroare reală, doar cleanup normal
            } catch (e: Exception) {
                Log.e(TAG, "Error saving workout to backend", e)
                onMessage("Eroare la salvarea workout-ului: ${e.message}")
            } finally {
                onSaveCompleted() // Apelă callback-ul pentru a continua navigarea
            }
        }
    }

    /**
     * Monitorizează timpul scurs în antrenament
     */
    private fun startElapsedTimeTracking() {
        viewModelScope.launch {
            while (_currentSession.value?.isActive == true) {
                if (_currentSession.value?.isPaused != true) {
                    val session = _currentSession.value
                    if (session != null) {
                        val elapsedSeconds = ((System.currentTimeMillis() - session.startTime) / 1000).toInt()
                        _currentSession.value = session.copy(elapsedTime = elapsedSeconds)
                    }
                }
                delay(1000)
            }
        }
    }

    /**
     * Convertește valoarea de putere din WorkoutStep în watts absolute
     * Suportă atât procente din FTP (0.6 = 60% FTP) cât și watts absolute (>10)
     */
    private fun convertToAbsolutePower(powerValue: Float): Int {
        return when {
            powerValue <= 0 -> 0
            powerValue <= 3.0f -> {
                // Tratează ca procent din FTP (ex: 0.6 = 60% din FTP, 1.2 = 120% din FTP)
                val absolutePower = (powerValue * userFtp).toInt()
                Log.d(TAG, "Power conversion: ${powerValue} x ${userFtp}W FTP = ${absolutePower}W")
                absolutePower
            }
            else -> {
                // Tratează ca watts absolute
                val absolutePower = powerValue.toInt()
                Log.d(TAG, "Power already absolute: ${absolutePower}W")
                absolutePower
            }
        }.coerceIn(0, 2000) // Limitează între 0-2000W pentru siguranță
    }

    /**
     * Determină zona de putere bazată pe watts și FTP
     */
    private fun calculatePowerZone(watts: Int): PowerZone {
        val ftpPercentage = if (userFtp > 0) (watts.toFloat() / userFtp) else 0f

        return when {
            ftpPercentage < 0.55f -> PowerZone.RECOVERY      // < 55% FTP
            ftpPercentage < 0.75f -> PowerZone.ENDURANCE     // 55-75% FTP
            ftpPercentage < 0.90f -> PowerZone.TEMPO         // 75-90% FTP
            ftpPercentage < 1.05f -> PowerZone.THRESHOLD     // 90-105% FTP
            ftpPercentage < 1.20f -> PowerZone.VO2_MAX       // 105-120% FTP
            else -> PowerZone.ANAEROBIC                      // > 120% FTP
        }
    }

    /**
     * Calculează procentajul din FTP pentru o putere dată
     */
    private fun calculatePowerZonePercentage(watts: Int): Int {
        return if (userFtp > 0) ((watts.toFloat() / userFtp) * 100).toInt() else 0
    }

    /**
     * Setează puterea țintă pe trainer bazată pe pasul curent cu control dinamic
     */
    private fun setCurrentStepPower() {
        val session = _currentSession.value ?: return
        val currentStep = session.trainingPlan.steps?.getOrNull(session.currentStep)

        // Oprește job-ul ERG anterior
        currentErgJob?.cancel()

        when (currentStep) {
            is WorkoutStep.SteadyState -> {
                val absolutePower = convertToAbsolutePower(currentStep.power)
                setTrainerResistance(absolutePower.toFloat())
                currentTargetPower = absolutePower
                _currentPowerZone.value = calculatePowerZone(absolutePower)
                _ergMode.value = true
                Log.d(TAG, "✓ SteadyState: Set trainer to ${absolutePower}W (${calculatePowerZonePercentage(absolutePower)}% FTP) - Zone: ${_currentPowerZone.value}")
            }
            is WorkoutStep.IntervalsT -> {
                startIntervalControl(currentStep)
            }
            is WorkoutStep.Ramp -> {
                startRampControl(currentStep)
            }
            is WorkoutStep.FreeRide -> {
                val lowPower = convertToAbsolutePower(currentStep.power_low)
                val highPower = convertToAbsolutePower(currentStep.power_high)
                val avgPower = (lowPower + highPower) / 2
                setTrainerResistance(avgPower.toFloat())
                currentTargetPower = avgPower
                _currentPowerZone.value = calculatePowerZone(avgPower)
                _ergMode.value = true
                Log.d(TAG, "✓ FreeRide: Set trainer to ${avgPower}W (${lowPower}W-${highPower}W range) - Zone: ${_currentPowerZone.value}")
            }
            is WorkoutStep.IntervalsP -> {
                startPowerIntervalControl(currentStep)
            }
            is WorkoutStep.Pyramid -> {
                startPyramidControl(currentStep)
            }
            else -> {
                _ergMode.value = false
                _currentPowerZone.value = PowerZone.RECOVERY
                Log.d(TAG, "No power target set for current step")
            }
        }
    }

    /**
     * Controlează intervalele cu timing automat
     */
    private fun startIntervalControl(step: WorkoutStep.IntervalsT) {
        _ergMode.value = true
        currentErgJob = viewModelScope.launch {
            repeat(step.repeat) { intervalIndex ->
                if (_currentSession.value?.isPaused != true) {
                    // Faza ON
                    val onPower = convertToAbsolutePower(step.on_power)
                    setTrainerResistance(onPower.toFloat())
                    currentTargetPower = onPower
                    _currentPowerZone.value = calculatePowerZone(onPower)
                    Log.d(TAG, "✓ Interval ${intervalIndex + 1}/${step.repeat} - ON: ${onPower}W (${calculatePowerZonePercentage(onPower)}% FTP) for ${step.on_duration}s")

                    delay(step.on_duration * 1000L)

                    if (_currentSession.value?.isPaused != true) {
                        // Faza OFF
                        val offPower = convertToAbsolutePower(step.off_power)
                        setTrainerResistance(offPower.toFloat())
                        currentTargetPower = offPower
                        _currentPowerZone.value = calculatePowerZone(offPower)
                        Log.d(TAG, "✓ Interval ${intervalIndex + 1}/${step.repeat} - OFF: ${offPower}W (${calculatePowerZonePercentage(offPower)}% FTP) for ${step.off_duration}s")

                        delay(step.off_duration * 1000L)
                    }
                }
            }
            Log.d(TAG, "Interval training completed")
        }
    }

    /**
     * Controlează ramp-ul cu progresie graduală
     */
    private fun startRampControl(step: WorkoutStep.Ramp) {
        _ergMode.value = true
        currentErgJob = viewModelScope.launch {
            val startPower = convertToAbsolutePower(step.start_power)
            val endPower = convertToAbsolutePower(step.end_power)
            val totalDuration = step.duration
            val powerDifference = endPower - startPower
            val updateInterval = 5 // Actualizează la fiecare 5 secunde
            val totalUpdates = totalDuration / updateInterval

            Log.d(TAG, "✓ Starting ramp: ${startPower}W (${calculatePowerZonePercentage(startPower)}% FTP) → ${endPower}W (${calculatePowerZonePercentage(endPower)}% FTP) over ${totalDuration}s")

            for (i in 0..totalUpdates) {
                if (_currentSession.value?.isPaused != true) {
                    val progress = i.toFloat() / totalUpdates
                    val currentPower = startPower + (powerDifference * progress).toInt()

                    setTrainerResistance(currentPower.toFloat())
                    currentTargetPower = currentPower
                    _currentPowerZone.value = calculatePowerZone(currentPower)
                    Log.d(TAG, "✓ Ramp progress: ${(progress * 100).toInt()}% - ${currentPower}W (${calculatePowerZonePercentage(currentPower)}% FTP) - Zone: ${_currentPowerZone.value}")

                    delay(updateInterval * 1000L)
                }
            }
            Log.d(TAG, "Ramp completed at ${endPower}W")
        }
    }

    /**
     * Controlează intervalele de putere
     */
    private fun startPowerIntervalControl(step: WorkoutStep.IntervalsP) {
        _ergMode.value = true
        currentErgJob = viewModelScope.launch {
            repeat(step.repeat) { intervalIndex ->
                if (_currentSession.value?.isPaused != true) {
                    // Faza ON
                    val onPower = convertToAbsolutePower(step.on_power)
                    setTrainerResistance(onPower.toFloat())
                    currentTargetPower = onPower
                    _currentPowerZone.value = calculatePowerZone(onPower)
                    Log.d(TAG, "✓ Power Interval ${intervalIndex + 1}/${step.repeat} - ON: ${onPower}W (${calculatePowerZonePercentage(onPower)}% FTP) for ${step.on_duration}s")

                    delay(step.on_duration * 1000L)

                    if (_currentSession.value?.isPaused != true) {
                        // Faza OFF
                        val offPower = convertToAbsolutePower(step.off_power)
                        setTrainerResistance(offPower.toFloat())
                        currentTargetPower = offPower
                        _currentPowerZone.value = calculatePowerZone(offPower)
                        Log.d(TAG, "✓ Power Interval ${intervalIndex + 1}/${step.repeat} - OFF: ${offPower}W (${calculatePowerZonePercentage(offPower)}% FTP) for ${step.off_duration}s")

                        delay(step.off_duration * 1000L)
                    }
                }
            }
            Log.d(TAG, "Power interval training completed")
        }
    }

    /**
     * Controlează piramida cu progresie în trepte
     */
    private fun startPyramidControl(step: WorkoutStep.Pyramid) {
        _ergMode.value = true
        currentErgJob = viewModelScope.launch {
            val startPower = convertToAbsolutePower(step.start_power)
            val peakPower = convertToAbsolutePower(step.peak_power)
            val endPower = convertToAbsolutePower(step.end_power)

            repeat(step.repeat) { pyramidIndex ->
                if (_currentSession.value?.isPaused != true) {
                    Log.d(TAG, "✓ Starting pyramid ${pyramidIndex + 1}/${step.repeat}: ${startPower}W → ${peakPower}W → ${endPower}W")

                    // Urcarea piramidei
                    val upSteps = 5
                    val upPowerStep = (peakPower - startPower) / upSteps

                    for (i in 0..upSteps) {
                        if (_currentSession.value?.isPaused != true) {
                            val currentPower = startPower + (upPowerStep * i)
                            setTrainerResistance(currentPower.toFloat())
                            currentTargetPower = currentPower
                            _currentPowerZone.value = calculatePowerZone(currentPower)
                            Log.d(TAG, "✓ Pyramid up step ${i + 1}/${upSteps + 1}: ${currentPower}W (${calculatePowerZonePercentage(currentPower)}% FTP)")
                            delay(step.step_duration * 1000L)
                        }
                    }

                    // Coborârea piramidei
                    val downSteps = 5
                    val downPowerStep = (peakPower - endPower) / downSteps

                    for (i in 1..downSteps) {
                        if (_currentSession.value?.isPaused != true) {
                            val currentPower = peakPower - (downPowerStep * i)
                            setTrainerResistance(currentPower.toFloat())
                            currentTargetPower = currentPower
                            _currentPowerZone.value = calculatePowerZone(currentPower)
                            Log.d(TAG, "✓ Pyramid down step $i/$downSteps: ${currentPower}W (${calculatePowerZonePercentage(currentPower)}% FTP)")
                            delay(step.step_duration * 1000L)
                        }
                    }
                }
            }
            Log.d(TAG, "Pyramid training completed")
        }
    }

    // TODO: Implementează calibrarea trainerului
    fun calibrateTrainer() {
        viewModelScope.launch {
            // Implementează logica de calibrare
        }
    }

    /**
     * Setează puterea țintă pe smart trainer cu ERG mode
     */
    fun setTrainerResistance(powerTarget: Float) {
        viewModelScope.launch {
            try {
                // Găsește primul trainer conectat (nu HR monitor)
                val trainerDevice = _connectedDevices.value?.firstOrNull { 
                    it.type != TrainerType.HEART_RATE_MONITOR && it.isConnected 
                }

                if (trainerDevice != null) {
                    val targetWatts = powerTarget.toInt().coerceIn(0, 2000) // Limitează între 0-2000W
                    val success = bleTrainerService.setTargetPower(trainerDevice.id, targetWatts)

                    if (success) {
                        currentTargetPower = targetWatts
                        Log.d(TAG, "✓ ERG: Successfully set trainer power to ${targetWatts}W on ${trainerDevice.name}")
                    } else {
                        Log.w(TAG, "✗ ERG: Failed to set trainer power to ${targetWatts}W - Control Point not available on ${trainerDevice.name}")
                    }
                } else {
                    Log.w(TAG, "✗ ERG: No trainer connected - cannot set power target")
                    _ergMode.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ ERG: Error setting trainer resistance to ${powerTarget.toInt()}W", e)
                _ergMode.value = false
            }
        }
    }

    /**
     * Obține puterea țintă curentă
     */
    fun getCurrentTargetPower(): Int = currentTargetPower

    /**
     * Verifică dacă ERG mode este activ
     */
    fun isErgModeActive(): Boolean = _ergMode.value == true

    /**
     * Obține FTP-ul curent
     */
    fun getCurrentFtp(): Int = userFtp

    /**
     * Obține zona de putere curentă ca string
     */
    fun getCurrentPowerZoneString(): String {
        return when (_currentPowerZone.value) {
            PowerZone.RECOVERY -> "Recovery (< 55% FTP)"
            PowerZone.ENDURANCE -> "Endurance (55-75% FTP)"
            PowerZone.TEMPO -> "Tempo (75-90% FTP)"
            PowerZone.THRESHOLD -> "Threshold (90-105% FTP)"
            PowerZone.VO2_MAX -> "VO2 Max (105-120% FTP)"
            PowerZone.ANAEROBIC -> "Anaerobic (> 120% FTP)"
            else -> "Unknown"
        }
    }
}
