package com.example.fitnessapp.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.model.HealthActivity
import com.example.fitnessapp.model.HealthConnectSyncRequest
import com.example.fitnessapp.model.HealthConnectSyncResponse
import com.example.fitnessapp.model.HeartRateData
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.random.Random

sealed class HealthConnectState {
    object NotConnected : HealthConnectState()
    object Connecting : HealthConnectState()
    object Connected : HealthConnectState()
    object Syncing : HealthConnectState()
    data class SyncingWithProgress(val currentBatch: Int, val totalBatches: Int, val activitiesSynced: Int) : HealthConnectState()
    data class Error(val message: String, val errorType: ErrorType = ErrorType.UNKNOWN, val details: String? = null) : HealthConnectState()
    object NotSupported : HealthConnectState()
    object PermissionRequired : HealthConnectState()
}

enum class ErrorType {
    NETWORK_ERROR,
    PERMISSION_ERROR,
    AUTHENTICATION_ERROR,
    BACKEND_ERROR,
    DATA_VALIDATION_ERROR,
    TIMEOUT_ERROR,
    HEALTH_CONNECT_ERROR,
    UNKNOWN
}

data class SyncMetrics(
    val startTime: Instant,
    var endTime: Instant? = null,
    var totalActivities: Int = 0,
    var successfulBatches: Int = 0,
    var failedBatches: Int = 0,
    var totalSynced: Int = 0,
    var errors: MutableList<String> = mutableListOf(),
    var networkChecks: Int = 0,
    var permissionChecks: Int = 0,
    var retryAttempts: Int = 0
) {
    fun getDurationMs(): Long = if (endTime != null) {
        endTime!!.toEpochMilli() - startTime.toEpochMilli()
    } else {
        Instant.now().toEpochMilli() - startTime.toEpochMilli()
    }

    fun getSuccessRate(): Double = if (totalActivities > 0) {
        (totalSynced.toDouble() / totalActivities.toDouble()) * 100.0
    } else 0.0
}

data class DebugSyncStatus(
    val lastSyncDate: Instant?,
    val hoursSinceLastSync: Long?,
    val currentState: HealthConnectState,
    val networkAvailable: Boolean,
    val networkType: String,
    val currentMetrics: SyncMetrics?,
    val recentLogs: List<String>,
    val healthConnectInstalled: Boolean,
    val permissionsGranted: Boolean?
)

data class HealthCheckResult(
    val isHealthy: Boolean,
    val issues: List<String>,
    val details: Map<String, Any>,
    val checkDurationMs: Long,
    val timestamp: Instant
)

data class DebugExport(
    val timestamp: Instant,
    val deviceInfo: Map<String, Any>,
    val syncInfo: Map<String, Any>,
    val recentLogs: List<String>,
    val healthConnectInstalled: Boolean,
    val networkAvailable: Boolean
)

enum class HealthConnectStatus {
    AVAILABLE_AND_CONNECTED,
    AVAILABLE_NOT_CONNECTED,
    NEEDS_SETUP,
    ERROR
}

class HealthConnectViewModel private constructor(
    private val applicationContext: Context
) : ViewModel() {

    private val _healthConnectState = MutableStateFlow<HealthConnectState>(HealthConnectState.NotConnected)
    val healthConnectState = _healthConnectState.asStateFlow()

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(applicationContext) }

    private val HC_PERMS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class)
    )

    private val healthConnectPackageName = "com.google.android.apps.healthdata"
    private val sharedPrefs = applicationContext.getSharedPreferences("health_connect_prefs", Context.MODE_PRIVATE)

    private var permissionCheckJob: Job? = null

    // Debugging and logging utilities
    private var currentSyncMetrics: SyncMetrics? = null
    private val debugLogs = mutableListOf<String>()
    private val maxDebugLogs = 100

    init {
        logDebug("HealthConnectViewModel initialized")
        checkAvailability()

        // Control periodic permission checking based on state
        viewModelScope.launch {
            _healthConnectState.collect { state ->
                if (state is HealthConnectState.PermissionRequired) {
                    // Permission checking will be started by onPermissionsDenied() if needed
                    logDebug("State is PermissionRequired")
                } else {
                    // Stop periodic checking for any other state
                    permissionCheckJob?.cancel()
                    if (state !is HealthConnectState.Connecting) {
                        logDebug("Stopping periodic permission check as state is no longer PermissionRequired")
                    }
                }
            }
        }
    }

    // ================================
    // DEBUGGING AND LOGGING UTILITIES
    // ================================

    private fun logDebug(message: String, tag: String = DEBUG_TAG) {
        Log.d(tag, message)
        addToDebugLog("[$tag] $message")
    }

    private fun logError(message: String, throwable: Throwable? = null, tag: String = ERROR_TAG) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
            addToDebugLog("[$tag] ERROR: $message - ${throwable.message}")
        } else {
            Log.e(tag, message)
            addToDebugLog("[$tag] ERROR: $message")
        }
    }

    private fun logPerformance(message: String, durationMs: Long? = null) {
        val logMessage = if (durationMs != null) {
            "$message (${durationMs}ms)"
        } else {
            message
        }
        Log.i(PERFORMANCE_TAG, logMessage)
        addToDebugLog("[$PERFORMANCE_TAG] $logMessage")
    }

    private fun logNetwork(message: String) {
        Log.i(NETWORK_TAG, message)
        addToDebugLog("[$NETWORK_TAG] $message")
    }

    private fun logPermission(message: String) {
        Log.i(PERMISSION_TAG, message)
        addToDebugLog("[$PERMISSION_TAG] $message")
    }

    private fun logData(message: String) {
        Log.i(DATA_TAG, message)
        addToDebugLog("[$DATA_TAG] $message")
    }

    private fun addToDebugLog(message: String) {
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val logEntry = "[$timestamp] $message"

        synchronized(debugLogs) {
            debugLogs.add(logEntry)
            if (debugLogs.size > maxDebugLogs) {
                debugLogs.removeAt(0)
            }
        }
    }

    fun getDebugLogs(): List<String> {
        return synchronized(debugLogs) {
            debugLogs.toList()
        }
    }

    fun clearDebugLogs() {
        synchronized(debugLogs) {
            debugLogs.clear()
        }
        logDebug("Debug logs cleared")
    }

    // Network connectivity checking
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.let {
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
                it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } ?: false
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        }
    }

    private fun getNetworkType(): String {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.typeName ?: "Unknown"
        }
    }

    private fun checkNetworkAndLog(): Boolean {
        val isAvailable = isNetworkAvailable()
        val networkType = getNetworkType()

        logNetwork("Network available: $isAvailable, Type: $networkType")

        currentSyncMetrics?.networkChecks = (currentSyncMetrics?.networkChecks ?: 0) + 1

        return isAvailable
    }

    // Health Connect permission verification with detailed logging
    private suspend fun verifyHealthConnectPermissions(): Boolean {
        return try {
            logPermission("Starting Health Connect permission verification")

            val healthConnectClient = HealthConnectClient.getOrCreate(applicationContext)
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()

            logPermission("Total permissions requested: ${HC_PERMS.size}")
            logPermission("Total permissions granted: ${grantedPermissions.size}")

            val missingPermissions = HC_PERMS - grantedPermissions

            if (missingPermissions.isNotEmpty()) {
                logPermission("Missing permissions: ${missingPermissions.map { it.toString() }}")
                currentSyncMetrics?.errors?.add("Missing permissions: ${missingPermissions.size}")
                false
            } else {
                logPermission("All required permissions are granted")
                true
            }
        } catch (e: Exception) {
            logError("Error verifying Health Connect permissions", e, PERMISSION_TAG)
            currentSyncMetrics?.errors?.add("Permission verification failed: ${e.message}")
            false
        } finally {
            currentSyncMetrics?.permissionChecks = (currentSyncMetrics?.permissionChecks ?: 0) + 1
        }
    }

    // Data validation with detailed logging
    private fun validateActivityData(activities: List<HealthActivity>): List<HealthActivity> {
        logData("Starting validation of ${activities.size} activities")

        val validActivities = mutableListOf<HealthActivity>()
        var invalidCount = 0

        activities.forEach { activity ->
            val validationErrors = mutableListOf<String>()

            // Validate required fields
            if (activity.id.isBlank()) {
                validationErrors.add("Empty ID")
            }

            if (activity.startTime.isBlank()) {
                validationErrors.add("Empty start time")
            }

            if (activity.duration < 0) {
                validationErrors.add("Negative duration: ${activity.duration}")
            }

            // Validate data ranges
            if (activity.steps < 0) {
                validationErrors.add("Negative steps: ${activity.steps}")
            }

            if (activity.calories < 0) {
                validationErrors.add("Negative calories: ${activity.calories}")
            }

            if (activity.distance < 0) {
                validationErrors.add("Negative distance: ${activity.distance}")
            }

            // Validate heart rate data
            activity.heartRateData.forEach { hrData ->
                if (hrData.beatsPerMinute <= 0 || hrData.beatsPerMinute > 300) {
                    validationErrors.add("Invalid heart rate: ${hrData.beatsPerMinute}")
                }
            }

            if (validationErrors.isNotEmpty()) {
                logData("Invalid activity ${activity.id}: ${validationErrors.joinToString(", ")}")
                invalidCount++
                currentSyncMetrics?.errors?.add("Invalid activity ${activity.id}: ${validationErrors.joinToString(", ")}")
            } else {
                validActivities.add(activity)
            }
        }

        logData("Validation complete: ${validActivities.size} valid, $invalidCount invalid")
        return validActivities
    }

    // Retry logic with exponential backoff
    private suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 10000,
        operation: suspend () -> T
    ): T {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                if (attempt > 0) {
                    currentSyncMetrics?.retryAttempts = (currentSyncMetrics?.retryAttempts ?: 0) + 1
                }

                return operation()
            } catch (e: Exception) {
                lastException = e

                if (attempt < maxRetries - 1) {
                    val delayMs = minOf(
                        initialDelayMs * (2.0.pow(attempt.toDouble())).toLong(),
                        maxDelayMs
                    )

                    logDebug("Attempt ${attempt + 1} failed, retrying in ${delayMs}ms: ${e.message}")
                    delay(delayMs)
                } else {
                    logError("All $maxRetries attempts failed", e)
                }
            }
        }

        throw lastException ?: Exception("Unknown error during retry operation")
    }

    // Sync metrics management
    private fun startSyncMetrics(): SyncMetrics {
        val metrics = SyncMetrics(startTime = Instant.now())
        currentSyncMetrics = metrics
        logPerformance("Sync metrics started")
        return metrics
    }

    private fun finishSyncMetrics(): SyncMetrics? {
        val metrics = currentSyncMetrics
        if (metrics != null) {
            metrics.endTime = Instant.now()
            logPerformance(
                "Sync completed - Duration: ${metrics.getDurationMs()}ms, " +
                "Success rate: ${"%.1f".format(metrics.getSuccessRate())}%, " +
                "Activities: ${metrics.totalSynced}/${metrics.totalActivities}, " +
                "Batches: ${metrics.successfulBatches}/${metrics.successfulBatches + metrics.failedBatches}, " +
                "Retries: ${metrics.retryAttempts}, " +
                "Errors: ${metrics.errors.size}"
            )
        }
        return metrics
    }

    fun getCurrentSyncMetrics(): SyncMetrics? = currentSyncMetrics

    // ================================
    // DEBUGGING UTILITY FUNCTIONS FOR UI
    // ================================

    /**
     * Get comprehensive sync status for debugging UI
     */
    fun getDebugSyncStatus(): DebugSyncStatus {
        val lastSync = getLastSyncDate()
        val metrics = currentSyncMetrics
        val networkAvailable = isNetworkAvailable()
        val networkType = getNetworkType()

        return DebugSyncStatus(
            lastSyncDate = lastSync,
            hoursSinceLastSync = lastSync?.let { ChronoUnit.HOURS.between(it, Instant.now()) },
            currentState = _healthConnectState.value,
            networkAvailable = networkAvailable,
            networkType = networkType,
            currentMetrics = metrics,
            recentLogs = getDebugLogs().takeLast(20),
            healthConnectInstalled = isHealthConnectInstalled(),
            permissionsGranted = null // Will be populated asynchronously
        )
    }

    /**
     * Perform comprehensive health check with detailed logging
     */
    suspend fun performHealthCheck(): HealthCheckResult {
        logDebug("=== STARTING HEALTH CHECK ===")
        val startTime = Instant.now()
        val issues = mutableListOf<String>()
        val details = mutableMapOf<String, Any>()

        try {
            // Check 1: Health Connect installation
            val isInstalled = isHealthConnectInstalled()
            details["healthConnectInstalled"] = isInstalled
            if (!isInstalled) {
                issues.add("Health Connect app not installed")
            }
            logDebug("Health Connect installed: $isInstalled")

            // Check 2: Network connectivity
            val networkAvailable = checkNetworkAndLog()
            details["networkAvailable"] = networkAvailable
            details["networkType"] = getNetworkType()
            if (!networkAvailable) {
                issues.add("No network connectivity")
            }

            // Check 3: Permissions
            val permissionsGranted = verifyHealthConnectPermissions()
            details["permissionsGranted"] = permissionsGranted
            details["totalPermissions"] = HC_PERMS.size
            if (!permissionsGranted) {
                issues.add("Health Connect permissions not granted")
            }

            // Check 4: Health Connect status
            val healthConnectStatus = checkHealthConnectStatus()
            details["healthConnectStatus"] = healthConnectStatus.toString()
            if (healthConnectStatus == HealthConnectStatus.ERROR) {
                issues.add("Health Connect status error")
            }

            // Check 5: Try reading a small amount of data
            try {
                val testActivities = readHealthConnectActivities(
                    Instant.now().minus(1, ChronoUnit.DAYS),
                    Instant.now()
                )
                details["testDataRead"] = true
                details["testActivitiesFound"] = testActivities.size
                logDebug("Test data read successful: ${testActivities.size} activities found")
            } catch (e: Exception) {
                details["testDataRead"] = false
                details["testDataError"] = e.message ?: "Unknown error"
                issues.add("Failed to read test data: ${e.message}")
                logError("Test data read failed", e)
            }

            val duration = Instant.now().toEpochMilli() - startTime.toEpochMilli()
            logPerformance("Health check completed", duration)

            return HealthCheckResult(
                isHealthy = issues.isEmpty(),
                issues = issues,
                details = details,
                checkDurationMs = duration,
                timestamp = Instant.now()
            )

        } catch (e: Exception) {
            logError("Health check failed", e)
            issues.add("Health check failed: ${e.message}")

            return HealthCheckResult(
                isHealthy = false,
                issues = issues,
                details = details,
                checkDurationMs = Instant.now().toEpochMilli() - startTime.toEpochMilli(),
                timestamp = Instant.now()
            )
        }
    }

    /**
     * Export debug information for support/troubleshooting
     */
    fun exportDebugInfo(): DebugExport {
        val deviceInfo = mapOf(
            "androidVersion" to Build.VERSION.RELEASE,
            "sdkVersion" to Build.VERSION.SDK_INT,
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "networkType" to getNetworkType()
        )

        val syncInfo = mapOf<String, Any>(
            "lastSyncDate" to (getLastSyncDate()?.toString() ?: "Never"),
            "currentState" to _healthConnectState.value.toString(),
            "currentMetrics" to (currentSyncMetrics?.toString() ?: "No metrics available")
        )

        return DebugExport(
            timestamp = Instant.now(),
            deviceInfo = deviceInfo,
            syncInfo = syncInfo,
            recentLogs = getDebugLogs(),
            healthConnectInstalled = isHealthConnectInstalled(),
            networkAvailable = isNetworkAvailable()
        )
    }

    /**
     * Force clear all sync data and reset to initial state
     */
    fun resetSyncState() {
        logDebug("=== RESETTING SYNC STATE ===")

        // Clear sync date
        resetSyncDate()

        // Clear metrics
        currentSyncMetrics = null

        // Clear debug logs
        clearDebugLogs()

        // Reset state
        _healthConnectState.value = HealthConnectState.NotConnected

        logDebug("Sync state reset completed")
    }

    // Error categorization
    private fun categorizeError(exception: Exception): ErrorType {
        return when {
            exception.message?.contains("network", ignoreCase = true) == true -> ErrorType.NETWORK_ERROR
            exception.message?.contains("timeout", ignoreCase = true) == true -> ErrorType.TIMEOUT_ERROR
            exception.message?.contains("permission", ignoreCase = true) == true -> ErrorType.PERMISSION_ERROR
            exception.message?.contains("authentication", ignoreCase = true) == true -> ErrorType.AUTHENTICATION_ERROR
            exception.message?.contains("401", ignoreCase = true) == true -> ErrorType.AUTHENTICATION_ERROR
            exception.message?.contains("403", ignoreCase = true) == true -> ErrorType.PERMISSION_ERROR
            exception.message?.contains("5", ignoreCase = true) == true -> ErrorType.BACKEND_ERROR
            exception is SecurityException -> ErrorType.PERMISSION_ERROR
            else -> ErrorType.UNKNOWN
        }
    }

    private fun createDetailedError(message: String, exception: Exception): HealthConnectState.Error {
        val errorType = categorizeError(exception)
        val details = buildString {
            append("Exception: ${exception.javaClass.simpleName}\n")
            append("Message: ${exception.message}\n")
            append("Network: ${getNetworkType()}\n")
            append("Time: ${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}\n")
            currentSyncMetrics?.let { metrics ->
                append("Sync duration: ${metrics.getDurationMs()}ms\n")
                append("Activities processed: ${metrics.totalSynced}/${metrics.totalActivities}\n")
            }
        }

        return HealthConnectState.Error(message, errorType, details)
    }

    private fun checkAvailability() {
        viewModelScope.launch {
            try {
                Log.d("HealthConnect", "Checking Health Connect availability and status")

                // Check if Health Connect app is installed
                val isHealthConnectInstalled = isHealthConnectInstalled()

                if (!isHealthConnectInstalled) {
                    Log.d("HealthConnect", "Health Connect not installed")
                    _healthConnectState.value = HealthConnectState.NotSupported
                    return@launch
                }

                // Check if Health Connect is actually accessible and has been set up
                val healthConnectStatus = checkHealthConnectStatus()

                when (healthConnectStatus) {
                    HealthConnectStatus.AVAILABLE_AND_CONNECTED -> {
                        Log.d("HealthConnect", "Health Connect is available and connected")
                        _healthConnectState.value = HealthConnectState.Connected
                    }
                    HealthConnectStatus.AVAILABLE_NOT_CONNECTED -> {
                        Log.d("HealthConnect", "Health Connect is available but not connected")
                        _healthConnectState.value = HealthConnectState.NotConnected
                    }
                    HealthConnectStatus.NEEDS_SETUP -> {
                        Log.d("HealthConnect", "Health Connect needs setup")
                        _healthConnectState.value = HealthConnectState.NotConnected
                    }
                    HealthConnectStatus.ERROR -> {
                        Log.e("HealthConnect", "Error checking Health Connect status")
                        _healthConnectState.value = HealthConnectState.Error("Unable to verify Health Connect status")
                    }
                }

            } catch (e: Exception) {
                Log.e("HealthConnect", "Failed to check availability", e)
                _healthConnectState.value = HealthConnectState.Error("Failed to check availability: ${e.message}")
            }
        }
    }

    private fun isHealthConnectInstalled(): Boolean {
        return try {
            applicationContext.packageManager.getPackageInfo(healthConnectPackageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private suspend fun checkHealthConnectStatus(): HealthConnectStatus {
        return try {
            Log.d("HealthConnect", "Checking real Health Connect status")

            // Check if Health Connect app is enabled and not just installed
            val packageInfo = applicationContext.packageManager.getPackageInfo(healthConnectPackageName, 0)
            val appInfo = packageInfo.applicationInfo

            if (appInfo?.enabled != true) {
                Log.d("HealthConnect", "Health Connect app is disabled or info unavailable")
                return HealthConnectStatus.NEEDS_SETUP
            }

            // Try to check if Health Connect has been set up by attempting to open it
            val canOpenHealthConnect = canOpenHealthConnect()
            if (!canOpenHealthConnect) {
                Log.d("HealthConnect", "Cannot open Health Connect - needs setup")
                return HealthConnectStatus.NEEDS_SETUP
            }

            // Check if we have any previous successful interaction
            val hasPermissions = hasAllPermissions()
            if (hasPermissions) {
                Log.d("HealthConnect", "Health Connect has permissions - connected")
                return HealthConnectStatus.AVAILABLE_AND_CONNECTED
            } else {
                Log.d("HealthConnect", "Health Connect available but no permissions")
                return HealthConnectStatus.AVAILABLE_NOT_CONNECTED
            }

        } catch (e: Exception) {
            Log.e("HealthConnect", "Error checking Health Connect status", e)
            HealthConnectStatus.ERROR
        }
    }

    private fun canOpenHealthConnect(): Boolean {
        return try {
            val intent = Intent().apply {
                action = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val activities = applicationContext.packageManager.queryIntentActivities(intent, 0)
            activities.isNotEmpty()
        } catch (e: Exception) {
            Log.e("HealthConnect", "Cannot check Health Connect settings intent", e)
            false
        }
    }

    private suspend fun hasAllPermissions(): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(HC_PERMS)
        } catch (e: Exception) {
            Log.e("HealthConnect", "Failed to get granted permissions", e)
            false
        }
    }

    fun connect() {
        viewModelScope.launch {
            try {
                Log.d("HealthConnect", "Starting Health Connect connection process")
                _healthConnectState.value = HealthConnectState.Connecting

                // Check if Health Connect is installed
                if (!isHealthConnectInstalled()) {
                    Log.e("HealthConnect", "Health Connect not installed")
                    _healthConnectState.value = HealthConnectState.NotSupported
                    return@launch
                }

                // Check current Health Connect status
                val currentStatus = checkHealthConnectStatus()
                Log.d("HealthConnect", "Health Connect Status: $currentStatus")

                when (currentStatus) {
                    HealthConnectStatus.AVAILABLE_AND_CONNECTED -> {
                        Log.d(
                            "HealthConnect",
                            "Health Connect available and already connected, verifying permissions"
                        )
                        if (hasAllPermissions()) {
                            _healthConnectState.value = HealthConnectState.Connected
                        } else {
                            Log.d(
                                "HealthConnect",
                                "Permissions needed for already connected state - UI will handle permission request"
                            )
                            // Stay in Connecting state, let UI handle permission request
                        }
                    }
                    HealthConnectStatus.AVAILABLE_NOT_CONNECTED -> {
                        Log.d(
                            "HealthConnect",
                            "Health Connect available, UI will request permissions"
                        )
                        // Stay in Connecting state, let UI handle permission request
                    }
                    HealthConnectStatus.NEEDS_SETUP -> {
                        Log.e("HealthConnect", "Health Connect needs setup")
                        _healthConnectState.value = HealthConnectState.Error("Health Connect needs to be set up first")
                    }
                    HealthConnectStatus.ERROR -> {
                        Log.e("HealthConnect", "Error accessing Health Connect")
                        _healthConnectState.value = HealthConnectState.Error("Unable to access Health Connect")
                    }
                }

            } catch (e: Exception) {
                Log.e("HealthConnect", "Connection failed", e)
                _healthConnectState.value = HealthConnectState.Error("Connection failed: ${e.message}")
            }
        }
    }

    fun startConnectionAttempt() {
        _healthConnectState.value = HealthConnectState.Connecting
    }

    private suspend fun requestHealthConnectPermissions() {
        try {
            Log.d("HealthConnect", "Requesting Health Connect permissions")
            _healthConnectState.value = HealthConnectState.PermissionRequired

            // Start periodic permission checking
            startPeriodicPermissionChecking()

        } catch (e: Exception) {
            Log.e("HealthConnect", "Permission request failed", e)
            _healthConnectState.value = HealthConnectState.Error("Permission request failed: ${e.message}")
        }
    }

    private fun startPeriodicPermissionChecking() {
        permissionCheckJob?.cancel()
        permissionCheckJob = viewModelScope.launch {
            Log.d(
                "HealthConnect",
                "Starting periodic permission checking (state is PermissionRequired)"
            )

            // Check permissions every 3 seconds for up to 2 minutes
            var checkCount = 0
            val maxChecks = 40 // 2 minutes / 3 seconds

            // Run only while state is PermissionRequired
            while (checkCount < maxChecks && _healthConnectState.value is HealthConnectState.PermissionRequired) {
                delay(3000) // Wait 3 seconds
                checkCount++

                Log.d("HealthConnect", "Periodic permission check #$checkCount")

                // Try to check if permissions are granted
                val hasPermissions = hasAllPermissions()

                if (hasPermissions) {
                    Log.d(
                        "HealthConnect",
                        "Permissions detected via periodic check - connection successful!"
                    )
                    _healthConnectState.value = HealthConnectState.Connected
                    val currentTime = System.currentTimeMillis()
                    sharedPrefs.edit()
                        .putLong("last_successful_read", currentTime)
                        .putBoolean("is_connected", true)
                        .putBoolean("user_granted_permissions", true)
                        .apply()
                    break
                } else {
                    Log.d("HealthConnect", "Permissions not yet granted, continuing to check...")
                }
            }

            // If we've reached max checks without success, stay in PermissionRequired state
            if (checkCount >= maxChecks && _healthConnectState.value is HealthConnectState.PermissionRequired) {
                Log.w(
                    "HealthConnect",
                    "Timeout waiting for permissions - user may still be considering"
                )
                // Keep state as PermissionRequired rather than Error
                // User can still grant permissions manually
            }
        }
    }

    private suspend fun attemptHealthConnectDataRead(): Boolean {
        return hasAllPermissions()
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                Log.d("HealthConnect", "Disconnecting from Health Connect")

                // Clear all saved Health Connect interaction data
                sharedPrefs.edit()
                    .remove("last_successful_read")
                    .remove("last_permission_check")
                    .remove("is_connected")
                    .remove("user_granted_permissions")
                    .apply()

                Log.d("HealthConnect", "Cleared local Health Connect state")
                _healthConnectState.value = HealthConnectState.NotConnected

            } catch (e: Exception) {
                Log.e("HealthConnect", "Disconnect failed", e)
                _healthConnectState.value = HealthConnectState.Error("Disconnect failed: ${e.message}")
            }
        }
    }

    // Callback methods for UI layer to handle permission results
    fun onPermissionsGranted() {
        viewModelScope.launch {
            Log.d("HealthConnect", "Permissions granted by user (callback from UI)")
            // Verify that permissions are actually granted
            if (hasAllPermissions()) {
                Log.d("HealthConnect", "Permissions verified - connection successful!")
                _healthConnectState.value = HealthConnectState.Connected
                val currentTime = System.currentTimeMillis()
                sharedPrefs.edit()
                    .putLong("last_successful_read", currentTime)
                    .putBoolean("is_connected", true)
                    .putBoolean("user_granted_permissions", true)
                    .apply()
            } else {
                Log.w("HealthConnect", "Permissions granted callback but verification failed.")
                _healthConnectState.value =
                    HealthConnectState.Error("Failed to verify permissions after granting. Please try again.")
            }
        }
    }

    fun onPermissionsDenied() {
        viewModelScope.launch {
            Log.d("HealthConnect", "Permissions denied by user (callback from UI)")
            _healthConnectState.value = HealthConnectState.PermissionRequired
            // Start periodic checking now that we're in PermissionRequired state
            startPeriodicPermissionChecking()
        }
    }

    fun initiateConnection(permissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>) {
        viewModelScope.launch {
            try {
                Log.d("HealthConnect", "Initiating connection to Health Connect")
                _healthConnectState.value = HealthConnectState.Connecting
                permissionLauncher.launch(HC_PERMS)
            } catch (e: Exception) {
                Log.e("HealthConnect", "Failed to launch permission request", e)
                _healthConnectState.value =
                    HealthConnectState.Error("Failed to open Health Connect. Please try again.")
            }
        }
    }

    fun openHealthConnectSettings() {
        try {
            Log.d("HealthConnect", "Attempting to open Health Connect permission request")

            // First try to open the specific permission request screen for our app
            val permissionIntent = Intent().apply {
                action = "androidx.health.ACTION_REQUEST_PERMISSIONS"
                setPackage(healthConnectPackageName)
                putExtra("calling_package", applicationContext.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Check if we can resolve the permission intent
            val canResolvePermissionIntent =
                applicationContext.packageManager.resolveActivity(permissionIntent, 0) != null

            if (canResolvePermissionIntent) {
                Log.d("HealthConnect", "Opening Health Connect permission request screen")
                applicationContext.startActivity(permissionIntent)
                return
            }

            // Fallback 1: Try to open Health Connect settings with app-specific data
            val settingsIntent = Intent().apply {
                action = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                // Add extra data to potentially direct to permissions
                putExtra("package_name", applicationContext.packageName)
                putExtra("calling_package", applicationContext.packageName)
            }

            val canResolveSettingsIntent =
                applicationContext.packageManager.resolveActivity(settingsIntent, 0) != null

            if (canResolveSettingsIntent) {
                Log.d("HealthConnect", "Opening Health Connect settings screen")
                applicationContext.startActivity(settingsIntent)
                return
            }

            // Fallback 2: Try to open Health Connect app directly
            Log.d("HealthConnect", "Trying fallback: opening Health Connect app directly")
            val intent =
                applicationContext.packageManager.getLaunchIntentForPackage(healthConnectPackageName)
            intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (intent != null) {
                applicationContext.startActivity(intent)
                Log.d("HealthConnect", "Successfully opened Health Connect app directly")
            } else {
                Log.e("HealthConnect", "Health Connect app launch intent is null")
            }

        } catch (e: Exception) {
            Log.e("HealthConnect", "Failed to open Health Connect", e)
            // Final fallback: try to open Health Connect app directly
            try {
                Log.d("HealthConnect", "Final fallback: opening Health Connect app directly")
                val intent = applicationContext.packageManager.getLaunchIntentForPackage(healthConnectPackageName)
                intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (intent != null) {
                    applicationContext.startActivity(intent)
                    Log.d("HealthConnect", "Successfully opened Health Connect app directly")
                } else {
                    Log.e("HealthConnect", "Health Connect app launch intent is null")
                }
            } catch (e2: Exception) {
                Log.e("HealthConnect", "Failed to open Health Connect app directly", e2)
            }
        }
    }

    fun verifyConnection() {
        viewModelScope.launch {
            try {
                Log.d("HealthConnect", "Manual verification of Health Connect connection requested")

                // Perform comprehensive verification
                val verificationResult = performDetailedVerification()

                if (verificationResult.isConnected) {
                    Log.d("HealthConnect", "Manual verification successful: ${verificationResult.details}")
                    _healthConnectState.value = HealthConnectState.Connected
                } else {
                    Log.d("HealthConnect", "Manual verification failed: ${verificationResult.details}")
                    _healthConnectState.value = HealthConnectState.Error("Verification failed: ${verificationResult.details}")
                }

            } catch (e: Exception) {
                Log.e("HealthConnect", "Manual verification failed with exception", e)
                _healthConnectState.value = HealthConnectState.Error("Verification error: ${e.message}")
            }
        }
    }

    private suspend fun performDetailedVerification(): VerificationResult {
        val details = mutableListOf<String>()

        // Step 1: Check if Health Connect app is installed
        val isInstalled = isHealthConnectInstalled()
        details.add("Health Connect installed: $isInstalled")
        if (!isInstalled) {
            return VerificationResult(false, details.joinToString(", "))
        }

        // Step 2: Check if Health Connect app is enabled
        val packageInfo = applicationContext.packageManager.getPackageInfo(healthConnectPackageName, 0)
        val isEnabled = packageInfo.applicationInfo?.enabled == true
        details.add("Health Connect enabled: $isEnabled")
        if (!isEnabled) {
            return VerificationResult(false, details.joinToString(", "))
        }

        // Step 3: Check if Health Connect settings can be opened
        val canOpenSettings = canOpenHealthConnect()
        details.add("Can open Health Connect settings: $canOpenSettings")
        if (!canOpenSettings) {
            return VerificationResult(false, details.joinToString(", "))
        }

        // Step 4: Check for recent successful interactions
        val lastSuccessfulRead = sharedPrefs.getLong("last_successful_read", 0)
        val lastPermissionCheck = sharedPrefs.getLong("last_permission_check", 0)
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursAgo = currentTime - (24 * 60 * 60 * 1000)

        val hasRecentInteraction = lastSuccessfulRead > twentyFourHoursAgo
        val hasRecentPermissionCheck = lastPermissionCheck > twentyFourHoursAgo

        details.add("Recent interaction (24h): $hasRecentInteraction")
        details.add("Recent permission check (24h): $hasRecentPermissionCheck")

        // Step 5: Try to verify Health Connect permissions through intent resolution
        val permissionIntentWorks = try {
            val intent = Intent().apply {
                action = "androidx.health.ACTION_REQUEST_PERMISSIONS"
                setPackage(healthConnectPackageName)
                putExtra("calling_package", applicationContext.packageName)
            }
            applicationContext.packageManager.resolveActivity(intent, 0) != null
        } catch (e: Exception) {
            false
        }
        details.add("Permission intent available: $permissionIntentWorks")

        // Step 6: Check if we can query Health Connect data intents
        val dataIntentWorks = try {
            val intent = Intent().apply {
                action = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
            }
            val activities = applicationContext.packageManager.queryIntentActivities(intent, 0)
            activities.isNotEmpty()
        } catch (e: Exception) {
            false
        }
        details.add("Data intent available: $dataIntentWorks")

        // Determine if connection is valid
        val isConnected = isInstalled && isEnabled && canOpenSettings &&
                (hasRecentInteraction || hasRecentPermissionCheck) &&
                (permissionIntentWorks || dataIntentWorks)

        // Update verification timestamp if successful
        if (isConnected) {
            sharedPrefs.edit().putLong("last_verification", currentTime).apply()
        }

        return VerificationResult(isConnected, details.joinToString(", "))
    }

    data class VerificationResult(
        val isConnected: Boolean,
        val details: String
    )

    // Health Connect data reading methods
    suspend fun readHealthConnectActivities(startDate: Instant? = null, endDate: Instant? = null): List<HealthActivity> {
        return try {
            val healthConnectClient = HealthConnectClient.getOrCreate(applicationContext)

            // Use provided dates or default to last 30 days for incremental sync
            val effectiveStartDate = startDate ?: Instant.now().minus(30, ChronoUnit.DAYS)
            val effectiveEndDate = endDate ?: Instant.now()

            Log.d("HealthConnect", "Reading activities from $effectiveStartDate to $effectiveEndDate")

            val exerciseRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(effectiveStartDate, effectiveEndDate)
                )
            )

            Log.d("HealthConnect", "Found ${exerciseRecords.records.size} exercise records")

            val activities = mutableListOf<HealthActivity>()

            for (record in exerciseRecords.records) {
                Log.d("HealthConnect", "Processing exercise: ${record.exerciseType} from ${record.startTime}")

                // Citește datele asociate pentru fiecare activitate
                val heartRateRecords = readHeartRateForSession(record.startTime, record.endTime)
                val stepsRecords = readStepsForSession(record.startTime, record.endTime)
                val caloriesRecords = readCaloriesForSession(record.startTime, record.endTime)
                val distanceRecords = readDistanceForSession(record.startTime, record.endTime)

                activities.add(
                    HealthActivity(
                        id = record.metadata.id ?: "unknown",
                        exerciseType = record.exerciseType,
                        startTime = record.startTime.toString(),
                        endTime = (record.endTime ?: record.startTime).toString(),
                        duration = if (record.endTime != null) {
                            record.endTime!!.epochSecond - record.startTime.epochSecond
                        } else 0L,
                        heartRateData = convertHeartRateRecords(heartRateRecords),
                        steps = stepsRecords.sumOf { it.count },
                        calories = caloriesRecords.sumOf { it.energy.inCalories },
                        distance = distanceRecords.sumOf { it.distance.inMeters },
                        title = record.title,
                        notes = record.notes
                    )
                )
            }

            Log.d("HealthConnect", "Successfully processed ${activities.size} activities")
            activities
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading activities from Health Connect", e)
            emptyList()
        }
    }

    // Helper function to convert HeartRateRecord to HeartRateData
    private fun convertHeartRateRecords(heartRateRecords: List<HeartRateRecord>): List<HeartRateData> {
        return heartRateRecords.flatMap { record ->
            record.samples.map { sample ->
                HeartRateData(
                    time = sample.time.toString(),
                    beatsPerMinute = sample.beatsPerMinute
                )
            }
        }
    }

    private suspend fun readHeartRateForSession(startTime: Instant, endTime: Instant): List<HeartRateRecord> {
        return try {
            val healthConnectClient = HealthConnectClient.getOrCreate(applicationContext)
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            ).records
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading heart rate data", e)
            emptyList()
        }
    }

    private suspend fun readStepsForSession(startTime: Instant, endTime: Instant): List<StepsRecord> {
        return try {
            val healthConnectClient = HealthConnectClient.getOrCreate(applicationContext)
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            ).records
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading steps data", e)
            emptyList()
        }
    }

    private suspend fun readCaloriesForSession(startTime: Instant, endTime: Instant): List<ActiveCaloriesBurnedRecord> {
        return try {
            val healthConnectClient = HealthConnectClient.getOrCreate(applicationContext)
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            ).records
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading calories data", e)
            emptyList()
        }
    }

    private suspend fun readDistanceForSession(startTime: Instant, endTime: Instant): List<DistanceRecord> {
        return try {
            val healthConnectClient = HealthConnectClient.getOrCreate(applicationContext)
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            ).records
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading distance data", e)
            emptyList()
        }
    }

    // Synchronization methods with comprehensive logging and debugging
    fun syncActivitiesToBackend(startDate: Instant? = null, endDate: Instant? = null, apiService: ApiService, getJwtToken: () -> String?) {
        viewModelScope.launch {
            val metrics = startSyncMetrics()

            try {
                logDebug("=== STARTING ENHANCED SMART SYNC PROCESS ===")
                logDebug("Sync parameters - startDate: $startDate, endDate: $endDate")
                _healthConnectState.value = HealthConnectState.Syncing

                // Step 1: Network connectivity check
                if (!checkNetworkAndLog()) {
                    val error = HealthConnectState.Error(
                        "No network connectivity available", 
                        ErrorType.NETWORK_ERROR,
                        "Network type: ${getNetworkType()}"
                    )
                    _healthConnectState.value = error
                    metrics.errors.add("Network connectivity failed")
                    finishSyncMetrics()
                    return@launch
                }

                // Step 2: Health Connect permissions verification
                if (!verifyHealthConnectPermissions()) {
                    val error = HealthConnectState.Error(
                        "Health Connect permissions not granted", 
                        ErrorType.PERMISSION_ERROR,
                        "Missing required Health Connect permissions"
                    )
                    _healthConnectState.value = error
                    metrics.errors.add("Permission verification failed")
                    finishSyncMetrics()
                    return@launch
                }

                // Step 3: JWT token verification
                val jwtToken = getJwtToken()
                logDebug("JWT Token available: ${jwtToken != null}")

                if (jwtToken == null) {
                    logError("No JWT token available - cannot sync", tag = ERROR_TAG)
                    val error = HealthConnectState.Error(
                        "No authentication token available", 
                        ErrorType.AUTHENTICATION_ERROR,
                        "JWT token is null or expired"
                    )
                    _healthConnectState.value = error
                    metrics.errors.add("JWT token unavailable")
                    finishSyncMetrics()
                    return@launch
                }

                // Step 4: Smart sync logic with detailed logging
                val lastSync = getLastSyncDate()
                var syncStartDate = startDate
                var syncEndDate = endDate ?: Instant.now()

                if (syncStartDate == null) {
                    if (lastSync != null) {
                        // Incremental sync: use last sync date but ensure minimum 24 hours
                        val minimumSyncPeriod = Instant.now().minus(24, ChronoUnit.HOURS)
                        syncStartDate = if (lastSync.isBefore(minimumSyncPeriod)) lastSync else minimumSyncPeriod
                        logDebug("Incremental sync with minimum 24h window from $syncStartDate to $syncEndDate")
                    } else {
                        // First sync: go back 30 days
                        syncStartDate = Instant.now().minus(30, ChronoUnit.DAYS)
                        logDebug("First-time sync from $syncStartDate to $syncEndDate")
                    }
                }

                // Step 5: Read activities with retry logic
                var activities = executeWithRetry(maxRetries = 3) {
                    readHealthConnectActivities(syncStartDate, syncEndDate)
                }
                logDebug("Found ${activities.size} activities in initial sync window")

                // Step 6: Smart window expansion if no activities found
                if (activities.isEmpty() && lastSync != null && startDate == null) {
                    logDebug("No activities found in incremental sync, expanding to 7 days")
                    syncStartDate = Instant.now().minus(7, ChronoUnit.DAYS)
                    activities = executeWithRetry(maxRetries = 2) {
                        readHealthConnectActivities(syncStartDate, syncEndDate)
                    }
                    logDebug("Found ${activities.size} activities in expanded 7-day window")

                    // If still no activities, try 30 days
                    if (activities.isEmpty()) {
                        logDebug("Still no activities found, expanding to 30 days")
                        syncStartDate = Instant.now().minus(30, ChronoUnit.DAYS)
                        activities = executeWithRetry(maxRetries = 2) {
                            readHealthConnectActivities(syncStartDate, syncEndDate)
                        }
                        logDebug("Found ${activities.size} activities in expanded 30-day window")
                    }
                }

                logDebug("Final sync window: $syncStartDate to $syncEndDate with ${activities.size} activities")
                metrics.totalActivities = activities.size

                if (activities.isNotEmpty()) {
                    // Step 7: Data validation
                    val validActivities = validateActivityData(activities)
                    logDebug("Data validation: ${validActivities.size}/${activities.size} activities are valid")

                    if (validActivities.isEmpty()) {
                        logError("All activities failed validation", tag = DATA_TAG)
                        val error = HealthConnectState.Error(
                            "All activities failed data validation", 
                            ErrorType.DATA_VALIDATION_ERROR,
                            "No valid activities to sync after validation"
                        )
                        _healthConnectState.value = error
                        metrics.errors.add("All activities failed validation")
                        finishSyncMetrics()
                        return@launch
                    }

                    // Step 8: Log sample activity for debugging
                    validActivities.firstOrNull()?.let { activity ->
                        logData("Sample activity: ID=${activity.id}, Type=${activity.exerciseType}, Duration=${activity.duration}s, Steps=${activity.steps}, Calories=${activity.calories}")
                    }

                    // Step 9: Process activities in batches with comprehensive error handling
                    val batchSize = 25 // Conservative batch size for better reliability
                    var totalSynced = 0

                    val batches = validActivities.chunked(batchSize)
                    logDebug("Processing ${batches.size} batches of up to $batchSize activities each")

                    for ((index, batch) in batches.withIndex()) {
                        // Update progress state
                        _healthConnectState.value = HealthConnectState.SyncingWithProgress(
                            currentBatch = index + 1,
                            totalBatches = batches.size,
                            activitiesSynced = totalSynced
                        )

                        logDebug("Syncing batch ${index + 1}/${batches.size} (${batch.size} activities)")

                        try {
                            // Network check before each batch
                            if (!isNetworkAvailable()) {
                                throw Exception("Network connectivity lost during sync")
                            }

                            val batchStartTime = Instant.now()

                            val syncRequest = HealthConnectSyncRequest(batch)
                            val response = executeWithRetry(maxRetries = 2) {
                                apiService.syncHealthConnectActivities("Bearer $jwtToken", syncRequest)
                            }

                            val batchDuration = Instant.now().toEpochMilli() - batchStartTime.toEpochMilli()
                            logPerformance("Batch ${index + 1} API call completed", batchDuration)
                            logDebug("Batch ${index + 1} response code: ${response.code()}")

                            if (response.isSuccessful) {
                                val syncResponse = response.body()
                                if (syncResponse?.success == true) {
                                    val syncedCount = syncResponse.syncedCount ?: batch.size
                                    totalSynced += syncedCount
                                    metrics.successfulBatches++
                                    metrics.totalSynced += syncedCount
                                    logDebug("✅ Batch ${index + 1} synced $syncedCount activities")
                                } else {
                                    val errorMsg = "Batch ${index + 1} sync failed: ${syncResponse?.message}"
                                    logError(errorMsg, tag = ERROR_TAG)
                                    metrics.failedBatches++
                                    metrics.errors.add(errorMsg)

                                    val error = HealthConnectState.Error(
                                        "Batch sync failed: ${syncResponse?.message}", 
                                        ErrorType.BACKEND_ERROR,
                                        "Batch ${index + 1} of ${batches.size} failed"
                                    )
                                    _healthConnectState.value = error
                                    finishSyncMetrics()
                                    return@launch
                                }
                            } else {
                                val errorBody = response.errorBody()?.string()
                                val errorMsg = "Batch ${index + 1} backend error: ${response.code()} - $errorBody"
                                logError(errorMsg, tag = ERROR_TAG)
                                metrics.failedBatches++
                                metrics.errors.add(errorMsg)

                                val errorType = when (response.code()) {
                                    401 -> ErrorType.AUTHENTICATION_ERROR
                                    403 -> ErrorType.PERMISSION_ERROR
                                    408, 504 -> ErrorType.TIMEOUT_ERROR
                                    in 500..599 -> ErrorType.BACKEND_ERROR
                                    else -> ErrorType.UNKNOWN
                                }

                                val error = HealthConnectState.Error(
                                    "Backend error: ${response.code()}", 
                                    errorType,
                                    errorBody ?: "No error details available"
                                )
                                _healthConnectState.value = error
                                finishSyncMetrics()
                                return@launch
                            }

                        } catch (e: Exception) {
                            val errorMsg = "Batch ${index + 1} processing failed: ${e.message}"
                            logError(errorMsg, e, ERROR_TAG)
                            metrics.failedBatches++
                            metrics.errors.add(errorMsg)

                            _healthConnectState.value = createDetailedError("Batch processing failed", e)
                            finishSyncMetrics()
                            return@launch
                        }

                        // Delay between batches to avoid overwhelming the server
                        if (index < batches.size - 1) {
                            delay(2000) // Increased delay for better reliability
                        }
                    }

                    // Step 10: Save sync timestamp and complete
                    saveLastSyncDate(Instant.now())
                    _healthConnectState.value = HealthConnectState.Connected

                    val finalMetrics = finishSyncMetrics()
                    logPerformance("✅ Successfully synced $totalSynced activities in ${batches.size} batches")
                    logDebug("Sync summary: ${finalMetrics?.let { "Duration: ${it.getDurationMs()}ms, Success rate: ${"%.1f".format(it.getSuccessRate())}%" } ?: "No metrics available"}")

                } else {
                    logDebug("⚠️ No new activities found to sync")
                    _healthConnectState.value = HealthConnectState.Connected
                    finishSyncMetrics()
                }

            } catch (e: Exception) {
                logError("❌ Critical error during sync process", e, ERROR_TAG)
                metrics.errors.add("Critical sync error: ${e.message}")
                _healthConnectState.value = createDetailedError("Sync process failed", e)
                finishSyncMetrics()
            }
        }
    }

    private fun getLastSyncDate(): Instant? {
        val timestamp = sharedPrefs.getLong("last_health_connect_sync", 0L)
        return if (timestamp > 0) Instant.ofEpochMilli(timestamp) else null
    }

    private fun saveLastSyncDate(date: Instant) {
        sharedPrefs.edit().putLong("last_health_connect_sync", date.toEpochMilli()).apply()
    }

    // Utility functions for sync management
    fun resetSyncDate() {
        Log.d("HealthConnect", "Resetting sync date - next sync will be a full historical sync")
        sharedPrefs.edit().remove("last_health_connect_sync").apply()
    }

    fun getSyncStatus(): SyncStatus {
        val lastSync = getLastSyncDate()
        return if (lastSync != null) {
            val hoursSinceLastSync = ChronoUnit.HOURS.between(lastSync, Instant.now())
            SyncStatus.HasSynced(lastSync, hoursSinceLastSync)
        } else {
            SyncStatus.NeverSynced
        }
    }

    sealed class SyncStatus {
        object NeverSynced : SyncStatus()
        data class HasSynced(val lastSyncDate: Instant, val hoursSinceLastSync: Long) : SyncStatus()
    }

    // Manual full sync for initial setup or when user wants all historical data
    fun syncAllActivitiesToBackend(apiService: ApiService, getJwtToken: () -> String?) {
        viewModelScope.launch {
            try {
                Log.d("HealthConnect", "=== STARTING FULL SYNC PROCESS ===")
                _healthConnectState.value = HealthConnectState.Syncing

                val jwtToken = getJwtToken()
                if (jwtToken == null) {
                    Log.e("HealthConnect", "No JWT token available - cannot sync")
                    _healthConnectState.value = HealthConnectState.Error("No authentication token available")
                    return@launch
                }

                // For full sync, read activities from last 6 months to avoid extreme data volumes
                val fullSyncStartDate = Instant.now().minus(180, ChronoUnit.DAYS) // 6 months
                val fullSyncEndDate = Instant.now()

                Log.d("HealthConnect", "Full sync from $fullSyncStartDate to $fullSyncEndDate")

                val activities = readHealthConnectActivities(fullSyncStartDate, fullSyncEndDate)
                Log.d("HealthConnect", "Found ${activities.size} activities for full sync")

                if (activities.isNotEmpty()) {
                    // Use smaller batch size for full sync to be more conservative
                    val batchSize = 25
                    var totalSynced = 0

                    val batches = activities.chunked(batchSize)
                    Log.d("HealthConnect", "Processing ${batches.size} batches of up to $batchSize activities each")

                    for ((index, batch) in batches.withIndex()) {
                        _healthConnectState.value = HealthConnectState.SyncingWithProgress(
                            currentBatch = index + 1,
                            totalBatches = batches.size,
                            activitiesSynced = totalSynced
                        )

                        Log.d("HealthConnect", "Full sync batch ${index + 1}/${batches.size} (${batch.size} activities)")

                        val syncRequest = HealthConnectSyncRequest(batch)
                        val response = apiService.syncHealthConnectActivities("Bearer $jwtToken", syncRequest)

                        if (response.isSuccessful) {
                            val syncResponse = response.body()
                            if (syncResponse?.success == true) {
                                totalSynced += syncResponse.syncedCount ?: batch.size
                                Log.d("HealthConnect", "✅ Full sync batch ${index + 1} synced ${syncResponse.syncedCount} activities")
                            } else {
                                Log.e("HealthConnect", "❌ Full sync batch ${index + 1} failed: ${syncResponse?.message}")
                                _healthConnectState.value = HealthConnectState.Error("Full sync failed: ${syncResponse?.message}")
                                return@launch
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("HealthConnect", "❌ Full sync batch ${index + 1} backend error: ${response.code()} - $errorBody")
                            _healthConnectState.value = HealthConnectState.Error("Backend error: ${response.code()}")
                            return@launch
                        }

                        // Longer delay between batches for full sync to be more conservative
                        if (index < batches.size - 1) {
                            delay(2000)
                        }
                    }

                    saveLastSyncDate(Instant.now())
                    _healthConnectState.value = HealthConnectState.Connected
                    Log.d("HealthConnect", "✅ Successfully completed full sync of $totalSynced activities in ${batches.size} batches")
                } else {
                    Log.w("HealthConnect", "⚠️ No activities found for full sync")
                    _healthConnectState.value = HealthConnectState.Connected
                }

            } catch (e: Exception) {
                Log.e("HealthConnect", "❌ Error during full sync", e)
                _healthConnectState.value = HealthConnectState.Error("Full sync error: ${e.message}")
            }
        }
    }

    fun setupAutoSync(apiService: ApiService, getJwtToken: () -> String?) {
        viewModelScope.launch {
            while (true) {
                delay(6 * 60 * 60 * 1000) // Sincronizează la fiecare 6 ore
                if (_healthConnectState.value is HealthConnectState.Connected) {
                    syncActivitiesToBackend(apiService = apiService, getJwtToken = getJwtToken)
                }
            }
        }
    }

    // Methods for new Health Connect endpoints
    suspend fun getHealthConnectActivities(
        apiService: ApiService, 
        getJwtToken: () -> String?,
        page: Int = 1,
        perPage: Int = 20,
        startDate: String? = null,
        endDate: String? = null
    ): List<com.example.fitnessapp.model.HealthConnectActivity>? {
        return try {
            val jwtToken = getJwtToken()
            if (jwtToken != null) {
                val response = apiService.getHealthConnectActivities(
                    "Bearer $jwtToken",
                    page,
                    perPage,
                    startDate,
                    endDate
                )

                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e("HealthConnect", "Error getting activities: ${response.code()}")
                    null
                }
            } else {
                Log.e("HealthConnect", "No authentication token available")
                null
            }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error getting activities", e)
            null
        }
    }

    suspend fun getHealthConnectStats(
        apiService: ApiService, 
        getJwtToken: () -> String?
    ): com.example.fitnessapp.model.HealthConnectStats? {
        return try {
            val jwtToken = getJwtToken()
            if (jwtToken != null) {
                val response = apiService.getHealthConnectStats("Bearer $jwtToken")

                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e("HealthConnect", "Error getting stats: ${response.code()}")
                    null
                }
            } else {
                Log.e("HealthConnect", "No authentication token available")
                null
            }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error getting stats", e)
            null
        }
    }

    suspend fun deleteHealthConnectActivity(
        apiService: ApiService, 
        getJwtToken: () -> String?,
        activityId: String
    ): Boolean {
        return try {
            val jwtToken = getJwtToken()
            if (jwtToken != null) {
                val response = apiService.deleteHealthConnectActivity(
                    "Bearer $jwtToken",
                    activityId
                )

                if (response.isSuccessful) {
                    Log.d("HealthConnect", "Activity deleted successfully")
                    true
                } else {
                    Log.e("HealthConnect", "Error deleting activity: ${response.code()}")
                    false
                }
            } else {
                Log.e("HealthConnect", "No authentication token available")
                false
            }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error deleting activity", e)
            false
        }
    }

    suspend fun getLastSync(
        apiService: ApiService, 
        getJwtToken: () -> String?
    ): com.example.fitnessapp.model.LastSyncResponse? {
        return try {
            val jwtToken = getJwtToken()
            if (jwtToken != null) {
                val response = apiService.getLastHealthConnectSync("Bearer $jwtToken")

                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e("HealthConnect", "Error getting last sync: ${response.code()}")
                    null
                }
            } else {
                Log.e("HealthConnect", "No authentication token available")
                null
            }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error getting last sync", e)
            null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: HealthConnectViewModel? = null

        // Logging constants
        private const val TAG = "HealthConnect"
        private const val DEBUG_TAG = "HealthConnect_DEBUG"
        private const val PERFORMANCE_TAG = "HealthConnect_PERF"
        private const val NETWORK_TAG = "HealthConnect_NET"
        private const val PERMISSION_TAG = "HealthConnect_PERM"
        private const val DATA_TAG = "HealthConnect_DATA"
        private const val ERROR_TAG = "HealthConnect_ERROR"

        fun getInstance(context: Context): HealthConnectViewModel {
            return INSTANCE ?: synchronized(this) {
                val instance = HealthConnectViewModel(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
