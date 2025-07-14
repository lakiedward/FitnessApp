package com.example.fitnessapp.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.fitnessapp.model.TrainerDevice
import com.example.fitnessapp.model.TrainerType
import com.example.fitnessapp.model.RealTimeData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.util.*

class BleTrainerService(private val context: Context) {
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null

    companion object {
        private const val TAG = "BleTrainerService"

        // UUID-uri standard pentru trainers și senzori fitness
        val CYCLING_POWER_SERVICE_UUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
        val CYCLING_POWER_MEASUREMENT_UUID = UUID.fromString("00002A63-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
        val FITNESS_MACHINE_SERVICE_UUID = UUID.fromString("00001826-0000-1000-8000-00805f9b34fb")
        val FITNESS_MACHINE_CONTROL_POINT_UUID = UUID.fromString("00002AD9-0000-1000-8000-00805f9b34fb")
        val CYCLING_SPEED_CADENCE_SERVICE_UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    /**
     * Scanează pentru dispozitive BLE trainer
     */
    @SuppressLint("MissingPermission")
    fun scanForTrainers(): Flow<TrainerDevice> = callbackFlow {
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions")
            close()
            return@callbackFlow
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth not available or not enabled")
            close()
            return@callbackFlow
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                try {
                    val device = result.device
                    val serviceUuids = result.scanRecord?.serviceUuids
                    val deviceName = if (hasBluetoothPermissions()) {
                        device.name ?: "Unknown Device"
                    } else {
                        "Unknown Device"
                    }

                    // Filtrează doar dispozitivele cu servicii relevante pentru fitness
                    val trainerType = when {
                        serviceUuids?.any { it.uuid == CYCLING_POWER_SERVICE_UUID } == true -> TrainerType.POWER_METER
                        serviceUuids?.any { it.uuid == FITNESS_MACHINE_SERVICE_UUID } == true -> TrainerType.SMART_TRAINER
                        serviceUuids?.any { it.uuid == HEART_RATE_SERVICE_UUID } == true -> TrainerType.HEART_RATE_MONITOR
                        serviceUuids?.any { it.uuid == CYCLING_SPEED_CADENCE_SERVICE_UUID } == true -> TrainerType.CADENCE_SENSOR
                        deviceName.contains("KICKR", ignoreCase = true) -> TrainerType.SMART_TRAINER
                        deviceName.contains("Direto", ignoreCase = true) -> TrainerType.SMART_TRAINER
                        deviceName.contains("Neo", ignoreCase = true) -> TrainerType.SMART_TRAINER
                        deviceName.contains("HRM", ignoreCase = true) -> TrainerType.HEART_RATE_MONITOR
                        deviceName.contains("Power", ignoreCase = true) -> TrainerType.POWER_METER
                        else -> null
                    }

                    if (trainerType != null) {
                        val trainerDevice = TrainerDevice(
                            id = device.address,
                            name = deviceName,
                            type = trainerType,
                            signalStrength = result.rssi
                        )

                        Log.d(TAG, "Found trainer device: $deviceName (${device.address}) - Type: $trainerType")
                        trySend(trainerDevice)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing scan result", e)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error code: $errorCode")
                close()
            }
        }

        // Configurează filtrul de scanare pentru servicii fitness
        val scanFilters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(CYCLING_POWER_SERVICE_UUID)).build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(FITNESS_MACHINE_SERVICE_UUID)).build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID)).build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(CYCLING_SPEED_CADENCE_SERVICE_UUID)).build()
        )

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        Log.d(TAG, "Starting BLE scan for trainer devices")
        bleScanner?.startScan(scanFilters, scanSettings, scanCallback)

        awaitClose { 
            Log.d(TAG, "Stopping BLE scan")
            bleScanner?.stopScan(scanCallback) 
        }
    }

    /**
     * Conectează la un dispozitiv trainer și returnează un flow cu date în timp real
     */
    @SuppressLint("MissingPermission")
    fun connectToTrainer(device: BluetoothDevice): Flow<RealTimeData> = callbackFlow {
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions for connection")
            close()
            return@callbackFlow
        }

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "Connected to ${device.name}")
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Disconnected from ${device.name}")
                        close()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered for ${device.name}")

                    // Abonează-te la notificările de putere
                    val powerService = gatt.getService(CYCLING_POWER_SERVICE_UUID)
                    powerService?.let { service ->
                        val powerCharacteristic = service.getCharacteristic(CYCLING_POWER_MEASUREMENT_UUID)
                        powerCharacteristic?.let { characteristic ->
                            enableNotifications(gatt, characteristic)
                        }
                    }

                    // Abonează-te la notificările de ritm cardiac
                    val heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID)
                    heartRateService?.let { service ->
                        val heartRateCharacteristic = service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
                        heartRateCharacteristic?.let { characteristic ->
                            enableNotifications(gatt, characteristic)
                        }
                    }

                    // Abonează-te la serviciul fitness machine
                    val fitnessService = gatt.getService(FITNESS_MACHINE_SERVICE_UUID)
                    fitnessService?.let { service ->
                        // Aici poți adăuga caracteristicile specifice pentru fitness machines
                        Log.d(TAG, "Fitness Machine service found")
                    }
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                when (characteristic.uuid) {
                    CYCLING_POWER_MEASUREMENT_UUID -> {
                        val powerData = parsePowerMeasurement(characteristic.value)
                        trySend(powerData)
                    }
                    HEART_RATE_MEASUREMENT_UUID -> {
                        val heartRateData = parseHeartRateMeasurement(characteristic.value)
                        trySend(heartRateData)
                    }
                }
            }
        }

        Log.d(TAG, "Connecting to ${device.name} (${device.address})")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)

        awaitClose { 
            Log.d(TAG, "Closing connection to ${device.name}")
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        }
    }

    /**
     * Activează notificările pentru o caracteristică
     */
    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        descriptor?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(it)
            Log.d(TAG, "Enabled notifications for ${characteristic.uuid}")
        }
    }

    /**
     * Parsează datele de putere conform standardului Bluetooth Cycling Power
     */
    private fun parsePowerMeasurement(data: ByteArray): RealTimeData {
        if (data.size < 4) return RealTimeData()

        try {
            // Flags (2 bytes) + Instantaneous Power (2 bytes)
            val power = ((data[3].toInt() and 0xFF) shl 8) or (data[2].toInt() and 0xFF)

            // Cadence poate fi prezentă în funcție de flags
            var cadence = 0
            if (data.size > 6) {
                cadence = data[6].toInt() and 0xFF
            }

            Log.d(TAG, "Power: ${power}W, Cadence: ${cadence}rpm")

            return RealTimeData(
                power = power,
                cadence = cadence,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing power measurement", e)
            return RealTimeData()
        }
    }

    /**
     * Parsează datele de ritm cardiac conform standardului Bluetooth Heart Rate
     */
    private fun parseHeartRateMeasurement(data: ByteArray): RealTimeData {
        if (data.isEmpty()) return RealTimeData()

        try {
            val flags = data[0].toInt() and 0xFF
            val heartRateFormat = flags and 0x01

            val heartRate = if (heartRateFormat == 0) {
                // 8-bit heart rate
                data[1].toInt() and 0xFF
            } else {
                // 16-bit heart rate
                ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            }

            Log.d(TAG, "Heart Rate: ${heartRate}bpm")

            return RealTimeData(
                heartRate = heartRate,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing heart rate measurement", e)
            return RealTimeData()
        }
    }

    /**
     * Verifică dacă aplicația are permisiunile Bluetooth necesare
     */
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Setează puterea țintă pe un smart trainer (dacă suportă)
     */
    @SuppressLint("MissingPermission")
    fun setTargetPower(powerWatts: Int): Boolean {
        val fitnessService = bluetoothGatt?.getService(FITNESS_MACHINE_SERVICE_UUID)
        val controlPoint = fitnessService?.getCharacteristic(FITNESS_MACHINE_CONTROL_POINT_UUID)

        return if (controlPoint != null) {
            // Construiește comanda pentru setarea puterii țintă
            val command = byteArrayOf(
                0x05, // Set Target Power opcode
                (powerWatts and 0xFF).toByte(),
                ((powerWatts shr 8) and 0xFF).toByte()
            )

            controlPoint.value = command
            bluetoothGatt?.writeCharacteristic(controlPoint) ?: false
        } else {
            Log.w(TAG, "Fitness Machine Control Point not available")
            false
        }
    }

    /**
     * Deconectează de la dispozitivul curent
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}
