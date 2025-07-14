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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import android.util.Log

class TrainerViewModel(private val context: Context) : ViewModel() {
    private val bleTrainerService = BleTrainerService(context)
    private var currentConnectedDevice: BluetoothDevice? = null
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

    companion object {
        private const val TAG = "TrainerViewModel"
    }

    init {
        _connectionState.value = ConnectionState.DISCONNECTED
        _realTimeData.value = RealTimeData()
    }

    /**
     * Scanează pentru dispozitive BLE trainer reale
     */
    fun scanForDevices() {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.SCANNING
                _availableDevices.value = emptyList()

                Log.d(TAG, "Starting BLE scan for trainer devices")

                // Colectează dispozitivele găsite timp de 15 secunde
                val foundDevices = mutableListOf<TrainerDevice>()
                var scanJob: kotlinx.coroutines.Job? = null

                scanJob = launch {
                    bleTrainerService.scanForTrainers()
                        .catch { exception ->
                            Log.e(TAG, "Error during BLE scan", exception)
                            _connectionState.value = ConnectionState.ERROR
                        }
                        .collect { device ->
                            // Evită duplicatele
                            if (!foundDevices.any { it.id == device.id }) {
                                foundDevices.add(device)
                                _availableDevices.value = foundDevices.toList()
                                Log.d(TAG, "Found device: ${device.name} (${device.type})")
                            }
                        }
                }

                // Timeout după 15 secunde
                delay(15000)

                // Oprește scanarea
                scanJob?.cancel()

                Log.d(TAG, "BLE scan completed. Found ${foundDevices.size} devices")
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
                    currentConnectedDevice = bluetoothDevice

                    // Conectează și începe să primească date în timp real
                    bleTrainerService.connectToTrainer(bluetoothDevice)
                        .catch { exception ->
                            Log.e(TAG, "Error connecting to trainer", exception)
                            _connectionState.value = ConnectionState.ERROR
                            currentConnectedDevice = null
                        }
                        .collect { realTimeData ->
                            // Actualizează datele în timp real
                            _realTimeData.value = realTimeData
                            Log.d(TAG, "Received data: Power=${realTimeData.power}W, HR=${realTimeData.heartRate}bpm")
                        }

                    // Marchează dispozitivul ca conectat
                    _connectedDevices.value = listOf(device.copy(isConnected = true))
                    _connectionState.value = ConnectionState.CONNECTED
                    Log.d(TAG, "Successfully connected to ${device.name}")

                } else {
                    Log.e(TAG, "Bluetooth device not found: ${device.id}")
                    _connectionState.value = ConnectionState.ERROR
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in connectToDevice", e)
                _connectionState.value = ConnectionState.ERROR
                currentConnectedDevice = null
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

                // Deconectează serviciul BLE
                bleTrainerService.disconnect()
                currentConnectedDevice = null

                // Actualizează starea
                val currentDevices = _connectedDevices.value ?: emptyList()
                _connectedDevices.value = currentDevices.filter { it.id != deviceId }

                if (_connectedDevices.value?.isEmpty() == true) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }

                Log.d(TAG, "Successfully disconnected from device: $deviceId")

            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting device", e)
            }
        }
    }

    fun startWorkout(trainingPlan: TrainingPlan) {
        val session = WorkoutSession(
            trainingPlan = trainingPlan,
            startTime = System.currentTimeMillis(),
            totalSteps = trainingPlan.steps?.size ?: 0
        )
        _currentSession.value = session.copy(isActive = true)

        // Începe monitorizarea timpului scurs
        startElapsedTimeTracking()

        // Setează puterea țintă pentru primul pas (dacă este disponibil)
        setCurrentStepPower()

        Log.d(TAG, "Started workout: ${trainingPlan.workout_name}")
    }

    fun pauseWorkout() {
        _currentSession.value = _currentSession.value?.copy(isPaused = true)
    }

    fun resumeWorkout() {
        _currentSession.value = _currentSession.value?.copy(isPaused = false)
    }

    fun stopWorkout() {
        _currentSession.value = null
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
     * Setează puterea țintă pe trainer bazată pe pasul curent
     */
    private fun setCurrentStepPower() {
        val session = _currentSession.value ?: return
        val currentStep = session.trainingPlan.steps?.getOrNull(session.currentStep)

        when (currentStep) {
            is WorkoutStep.SteadyState -> {
                setTrainerResistance(currentStep.power)
                Log.d(TAG, "Set trainer power to ${currentStep.power}W for SteadyState")
            }
            is WorkoutStep.IntervalsT -> {
                // Pentru intervale, începe cu puterea ON
                setTrainerResistance(currentStep.on_power)
                Log.d(TAG, "Set trainer power to ${currentStep.on_power}W for Intervals")
            }
            is WorkoutStep.Ramp -> {
                // Pentru ramp, începe cu puterea de start
                setTrainerResistance(currentStep.start_power)
                Log.d(TAG, "Set trainer power to ${currentStep.start_power}W for Ramp start")
            }
            is WorkoutStep.FreeRide -> {
                // Pentru free ride, setează puterea medie
                val avgPower = (currentStep.power_low + currentStep.power_high) / 2
                setTrainerResistance(avgPower)
                Log.d(TAG, "Set trainer power to ${avgPower}W for FreeRide")
            }
            is WorkoutStep.IntervalsP -> {
                setTrainerResistance(currentStep.on_power)
                Log.d(TAG, "Set trainer power to ${currentStep.on_power}W for Power Intervals")
            }
            is WorkoutStep.Pyramid -> {
                setTrainerResistance(currentStep.start_power)
                Log.d(TAG, "Set trainer power to ${currentStep.start_power}W for Pyramid start")
            }
            else -> {
                Log.d(TAG, "No power target set for current step")
            }
        }
    }

    // TODO: Implementează calibrarea trainerului
    fun calibrateTrainer() {
        viewModelScope.launch {
            // Implementează logica de calibrare
        }
    }

    /**
     * Setează puterea țintă pe smart trainer
     */
    fun setTrainerResistance(powerTarget: Float) {
        viewModelScope.launch {
            try {
                if (currentConnectedDevice != null) {
                    val success = bleTrainerService.setTargetPower(powerTarget.toInt())
                    if (success) {
                        Log.d(TAG, "Successfully set trainer power to ${powerTarget.toInt()}W")
                    } else {
                        Log.w(TAG, "Failed to set trainer power to ${powerTarget.toInt()}W")
                    }
                } else {
                    Log.w(TAG, "No trainer connected - cannot set power target")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting trainer resistance", e)
            }
        }
    }
}
