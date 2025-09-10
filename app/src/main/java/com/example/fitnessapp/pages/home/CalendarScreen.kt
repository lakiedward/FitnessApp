package com.example.fitnessapp.pages.home

import com.example.fitnessapp.R

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

import com.example.fitnessapp.api.ApiConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.model.StravaActivity
import com.example.fitnessapp.model.TrainingDateUpdate
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.model.WorkoutStep
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Modele pentru răspunsul /strava/comprehensive-analysis
data class ComprehensiveAnalysisResponse(
    val cycling_fthr: FthrData? = null,
    val running_fthr: FthrData? = null,
    val swimming_fthr: FthrData? = null,
    val other_fthr: FthrData? = null,
    val hrtss_calculation: HrtssData? = null,
    val ftp_estimation: FtpData? = null,
    val running_pace_predictions: List<RunningPacePrediction>? = null,
    val swimming_best_times: List<SwimmingBestTime>? = null,
    val summary: SummaryData? = null
)

data class FthrData(
    val estimated_fthr: Int? = null,
    val source: String? = null,
    val activities_used: Int? = null,
    val max_hr_observed: Int? = null,
    val updated_at: String? = null,
    val error: String? = null  // Pentru erori per secțiune
)

data class HrtssData(
    val status: String? = null,
    val updated: Int? = null,
    val error: String? = null
)

data class FtpData(
    val date: String? = null,
    val estimated_ftp: Int? = null,
    val confidence: Double? = null,
    val source_activities: List<Long>? = null,
    val method: String? = null,
    val notes: String? = null,
    val confidence_metrics: Map<String, Any>? = null,
    val fthr_value: Int? = null,
    val fthr_source: String? = null,
    val fthr_age_days: Int? = null,
    val weekly_ftp: List<WeeklyFtp>? = null,
    val error: String? = null
)

data class WeeklyFtp(
    val week: Int? = null,
    val week_end: String? = null,
    val ftp_20min_est: Int? = null,
    val ftp_hr_est: Int? = null,
    val tss_week: Int? = null,
    val ftp_tss_est: Int? = null,
    val ftp_final: Int? = null,
    val ftp_final_wkg: Double? = null,
    val categorie: String? = null,
    val stare_saptamana: String? = null
)

data class RunningPacePrediction(
    val distance_km: Double? = null,
    val time: String? = null,
    val pace_min_per_km: Double? = null,
    val avg_hr: Double? = null,
    val adjusted_for_hr: Double? = null,
    val error: String? = null
)

data class SwimmingBestTime(
    val distance_m: Int? = null,
    val time: String? = null,
    val error: String? = null
)

data class SummaryData(
    val total_operations: Int? = null,
    val successful_operations: Int? = null,
    val failed_operations: Int? = null,
    val completion_rate: String? = null,
    val timestamp: String? = null,
    val error: String? = null
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InfiniteCalendarPage(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val trainingPlans by authViewModel.trainingPlan.observeAsState(emptyList())
    Log.d("CalendarScreen", "Training Plans: $trainingPlans")

    val stravaViewModel: StravaViewModel =
        viewModel(factory = StravaViewModelFactory(LocalContext.current))
    val stravaActivities by stravaViewModel.stravaActivities.collectAsState()

    // Unified activities state (from new endpoint)
    var isInitialLoading by remember { mutableStateOf(true) }
    var unifiedActivities by remember { mutableStateOf<List<StravaActivity>>(emptyList()) }
    var isActivitiesLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Sync live state
    var isSyncingLive by remember { mutableStateOf(false) }
    var syncCurrentActivity by remember { mutableStateOf<String?>(null) }
    var syncActivitiesCount by remember { mutableStateOf(0) }
    var syncProgress by remember { mutableStateOf(0f) }
    var syncError by remember { mutableStateOf<String?>(null) }

    // Snackbar host state for non-blocking notifications
    val snackbarHostState = remember { SnackbarHostState() }

    Log.d("CalendarScreen", "Strava Activities: $stravaActivities")

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = Int.MAX_VALUE / 2)
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTrainingPlan by remember { mutableStateOf<TrainingPlan?>(null) }
    var showMapDialog by remember { mutableStateOf(false) }
    var selectedActivityId by remember { mutableStateOf<Long?>(null) }

    // Function to calculate date range and fetch unified activities from backend
    fun loadActivitiesForDateRange(isInitialLoad: Boolean, onFinished: () -> Unit = {}) {
        if (isActivitiesLoading) return
        coroutineScope.launch {
            try {
                isActivitiesLoading = true
                val startCalendar: Calendar
                val endCalendar: Calendar
                val bufferDays = if (isInitialLoad) 7 else 30

                if (isInitialLoad) {
                    startCalendar = Calendar.getInstance().apply {
                        time = today.time
                        add(Calendar.DAY_OF_MONTH, -bufferDays)
                    }
                    endCalendar = Calendar.getInstance().apply {
                        time = today.time
                        add(Calendar.DAY_OF_MONTH, bufferDays)
                    }
                } else {
                    val firstVisibleIndex = listState.firstVisibleItemIndex
                    val visibleItemCount = listState.layoutInfo.visibleItemsInfo.size

                    val startIndex = firstVisibleIndex - bufferDays
                    val endIndex = firstVisibleIndex + visibleItemCount + bufferDays

                    startCalendar = Calendar.getInstance().apply {
                        time = today.time
                        add(Calendar.DAY_OF_MONTH, (startIndex - Int.MAX_VALUE / 2).toInt())
                    }

                    endCalendar = Calendar.getInstance().apply {
                        time = today.time
                        add(Calendar.DAY_OF_MONTH, (endIndex - Int.MAX_VALUE / 2).toInt())
                    }
                }

                val startDate =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startCalendar.time)
                val endDate =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endCalendar.time)

                Log.d(
                    "CalendarScreen",
                    "Loading unified activities from $startDate to $endDate (isInitial: $isInitialLoad)"
                )

                // Bound the fetch to avoid long hangs due to backend timeouts
                val timeoutMs = if (isInitialLoad) 22_000L else 12_000L
                var activities = try {
                    withTimeout(timeoutMs) {
                        stravaViewModel.getUnifiedActivities(startDate, endDate)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w("CalendarScreen", "Unified activities load timed out for $startDate..$endDate; trying DB fallback")
                    if (isInitialLoad) {
                        snackbarHostState.showSnackbar(
                            message = "Loading activities timed out. Trying fallback...",
                            duration = SnackbarDuration.Short
                        )
                    }
                    // Fallback to DB-only list (faster, may omit app workouts)
                    val dbFallback = withTimeoutOrNull(12_000) {
                        stravaViewModel.getActivitiesFromDatabase(startDate, endDate)
                    } ?: emptyList()
                    if (dbFallback.isNotEmpty()) {
                        Log.d("CalendarScreen", "Loaded ${dbFallback.size} activities via DB fallback")
                    }
                    dbFallback
                } catch (e: CancellationException) {
                    // Ignore UX noise when composition cancels ongoing work (e.g., fast scroll)
                    Log.d("CalendarScreen", "Unified activities load cancelled")
                    return@launch
                }
                // If unified endpoint responded but returned empty, try a quick DB fallback to show history
                if (activities.isEmpty()) {
                    val quickDb = withTimeoutOrNull(8_000) {
                        stravaViewModel.getActivitiesFromDatabase(startDate, endDate)
                    } ?: emptyList()
                    if (quickDb.isNotEmpty()) {
                        Log.d("CalendarScreen", "Unified empty; using ${quickDb.size} DB activities")
                        activities = quickDb
                    }
                }
                if (activities.isNotEmpty()) {
                    unifiedActivities = activities
                    Log.d("CalendarScreen", "Loaded ${activities.size} unified activities")
                } else {
                    Log.d("CalendarScreen", "Activities result empty; keeping previous list of ${unifiedActivities.size}")
                }
            } catch (e: Exception) {
                Log.e("CalendarScreen", "Error loading unified activities", e)
            } finally {
                isActivitiesLoading = false
                onFinished()
            }
        }
    }


    // Function to perform sync live
    fun performSyncLive() {
        if (isSyncingLive) return

        isSyncingLive = true
        syncCurrentActivity = null
        syncActivitiesCount = 0
        syncProgress = 0f
        syncError = null

        GlobalScope.launch {
            try {
                // Show initial sync start notification
                snackbarHostState.showSnackbar(
                    message = "Sincronizare Strava începută...",
                    duration = SnackbarDuration.Short
                )

                val jwtToken = authViewModel.getToken()
                if (jwtToken.isNullOrEmpty()) {
                    snackbarHostState.showSnackbar(
                        "Token de autentificare lipsă", 
                        duration = SnackbarDuration.Long
                    )
                    return@launch
                }

                Log.d("CalendarScreen", "Starting sync live with token")

                val fullUrl = "${ApiConfig.BASE_URL}strava/sync-live"
                val gson = Gson()

                // Variables for reconnection logic
                var totalActivitiesSynced = 0
                var maxRetries = 3
                var currentRetry = 0
                var isSyncCompleted = false

                while (!isSyncCompleted && currentRetry < maxRetries) {
                    if (currentRetry > 0) {
                        Log.d("CalendarScreen", "Reconnection attempt $currentRetry/$maxRetries")
                        snackbarHostState.showSnackbar(
                            "Reconectare... (încercarea $currentRetry/$maxRetries)",
                            duration = SnackbarDuration.Short
                        )
                        delay(2000)
                    }

                    withContext(Dispatchers.IO) {
                        val url = URL(fullUrl)
                        val connection = url.openConnection() as HttpURLConnection

                        try {
                            connection.requestMethod = "GET"
                            connection.setRequestProperty("Authorization", "Bearer $jwtToken")
                            connection.setRequestProperty("Accept", "text/event-stream")
                            connection.setRequestProperty("Cache-Control", "no-cache")
                            connection.setRequestProperty("Connection", "keep-alive")
                            connection.connectTimeout = 30000
                            connection.readTimeout = 0

                            Log.d(
                                "CalendarScreen",
                                "Making SSE request (attempt ${currentRetry + 1})"
                            )

                            if (connection.responseCode != 200) {
                                val errorBody =
                                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                                Log.e(
                                    "CalendarScreen",
                                    "SSE request failed: ${connection.responseCode} - $errorBody"
                                )
                                throw Exception("Sincronizare eșuată: ${connection.responseCode}")
                            }

                            val inputStream = connection.inputStream
                            val reader = BufferedReader(InputStreamReader(inputStream))

                            var line: String?
                            var activitiesCount = 0

                            withContext(Dispatchers.Main) {
                                syncCurrentActivity = "Sincronizare începută..."
                                syncProgress = 0.1f
                            }

                            while (reader.readLine().also { line = it } != null) {
                                val currentLine = line?.trim()

                                // Skip empty lines and comments
                                if (currentLine.isNullOrEmpty() || currentLine.startsWith(":")) {
                                    continue
                                }

                                // Process only data lines
                                if (currentLine.startsWith("data: ")) {
                                    val jsonData =
                                        currentLine.substring(6) // Remove "data: " prefix

                                    if (jsonData.trim().isEmpty()) {
                                        continue // Skip empty data lines
                                    }

                                    try {
                                        val eventData =
                                            gson.fromJson(jsonData, JsonObject::class.java)
                                        Log.d("CalendarScreen", "Received SSE event: $eventData")

                                        // Handle completion status
                                        if (eventData.has("status") && eventData.get("status").asString == "done") {
                                            Log.d("CalendarScreen", "Sync completed successfully!")
                                            snackbarHostState.showSnackbar(
                                                "Sincronizare completă! ${totalActivitiesSynced + activitiesCount} activități",
                                                duration = SnackbarDuration.Long
                                            )

                                            // Reload activities after successful sync
                                            if (totalActivitiesSynced + activitiesCount > 0) {
                                                Log.d(
                                                    "CalendarScreen",
                                                    "Reloading activities after successful sync with ${totalActivitiesSynced + activitiesCount} activities"
                                                )
                                                loadActivitiesForDateRange(isInitialLoad = false)
                                            }

                                            isSyncCompleted = true
                                            break
                                        }

                                        // Handle errors
                                        if (eventData.has("error")) {
                                            val errorMessage = eventData.get("error").asString
                                            Log.e(
                                                "CalendarScreen",
                                                "SSE error received: $errorMessage"
                                            )
                                            snackbarHostState.showSnackbar(
                                                "Eroare: $errorMessage",
                                                duration = SnackbarDuration.Long
                                            )
                                            throw Exception(errorMessage)
                                        }

                                        // Handle individual activity sync events
                                        if (eventData.has("name") && eventData.has("start_date")) {
                                            activitiesCount++
                                            val activityName = eventData.get("name").asString
                                            totalActivitiesSynced = activitiesCount // Update total count

                                            Log.d(
                                                "CalendarScreen",
                                                "Activity synced: $activityName"
                                            )

                                            // Show snackbar notification for each synchronized activity
                                            snackbarHostState.showSnackbar(
                                                "Activitate sincronizată: $activityName",
                                                duration = SnackbarDuration.Short
                                            )
                                            delay(150) // Small delay between notifications
                                        }

                                    } catch (e: Exception) {
                                        Log.e(
                                            "CalendarScreen",
                                            "Error parsing SSE event: $jsonData",
                                            e
                                        )
                                        // Continue processing other events
                                    }
                                }
                            }

                            // If we reach here without completion, connection was dropped
                            if (!isSyncCompleted) {
                                Log.w(
                                    "CalendarScreen",
                                    "SSE connection dropped after syncing $activitiesCount activities"
                                )
                                totalActivitiesSynced += activitiesCount
                                currentRetry++

                                snackbarHostState.showSnackbar(
                                    "Conexiunea s-a întrerupt. Reconectare...",
                                    duration = SnackbarDuration.Short
                                )
                            } else {
                                // Connection completed successfully
                                Log.d("CalendarScreen", "SSE connection completed successfully")
                            }

                        } catch (e: Exception) {
                            Log.e(
                                "CalendarScreen",
                                "Error during SSE sync (attempt ${currentRetry + 1})",
                                e
                            )

                            // Classify error types for better handling
                            val isConnectionError = e.message?.let { msg ->
                                msg.contains("Connection", ignoreCase = true) ||
                                        msg.contains("timeout", ignoreCase = true) ||
                                        msg.contains("network", ignoreCase = true) ||
                                        msg.contains("unreachable", ignoreCase = true)
                            } ?: false

                            if (isConnectionError && currentRetry < maxRetries - 1) {
                                currentRetry++
                                snackbarHostState.showSnackbar(
                                    "Problemă de conexiune. Reconectare...",
                                    duration = SnackbarDuration.Short
                                )
                                Log.w("CalendarScreen", "Connection error detected, will retry")
                            } else {
                                // For non-connection errors or max retries reached, stop
                                throw e
                            }
                        } finally {
                            try {
                                connection.disconnect()
                            } catch (e: Exception) {
                                Log.w("CalendarScreen", "Error disconnecting: ${e.message}")
                            }
                        }
                    }
                }

                // If exhausted retries without completion
                if (!isSyncCompleted) {
                    val message =
                        "Sincronizarea s-a întrerupt după $maxRetries încercări. $totalActivitiesSynced activități sincronizate."
                    Log.w("CalendarScreen", message)
                    snackbarHostState.showSnackbar(
                        message,
                        duration = SnackbarDuration.Long
                    )
                } else {
                    // Dacă sync complet, rulează analiza comprehensivă
                    snackbarHostState.showSnackbar(
                        "Analiză comprehensivă începută...",
                        duration = SnackbarDuration.Short
                    )

                    // Apel la /strava/comprehensive-analysis
                    val analysisResponse = withContext(Dispatchers.IO) {
                        val url = URL("${ApiConfig.BASE_URL}strava/comprehensive-analysis")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("Authorization", "Bearer $jwtToken")
                        connection.setRequestProperty("Accept", "application/json")
                        connection.connectTimeout = 30000
                        connection.readTimeout = 60000  // 60s, deoarece poate dura

                        if (connection.responseCode == 200) {
                            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                            Log.d("CalendarScreen", "Analysis response: $responseBody")
                            gson.fromJson(responseBody, ComprehensiveAnalysisResponse::class.java)
                        } else {
                            val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                            Log.e("CalendarScreen", "Analysis failed: ${connection.responseCode} - $errorBody")
                            null
                        }
                    }

                    // Gestionează răspunsul și arată notificări
                    if (analysisResponse != null) {
                        // Exemplu: Notificări per secțiune (poți extinde cu toate)
                        analysisResponse.cycling_fthr?.let { fthr ->
                            if (fthr.error != null) {
                                snackbarHostState.showSnackbar("Eroare FTHR ciclism: ${fthr.error}", duration = SnackbarDuration.Long)
                            } else {
                                snackbarHostState.showSnackbar("FTHR ciclism: ${fthr.estimated_fthr} bpm (activități: ${fthr.activities_used})", duration = SnackbarDuration.Short)
                            }
                        }

                        analysisResponse.ftp_estimation?.let { ftp ->
                            if (ftp.error != null) {
                                snackbarHostState.showSnackbar("Eroare FTP: ${ftp.error}", duration = SnackbarDuration.Long)
                            } else {
                                snackbarHostState.showSnackbar("FTP estimat: ${ftp.estimated_ftp}W (încredere: ${(ftp.confidence ?: 0.0) * 100}%)", duration = SnackbarDuration.Short)
                            }
                        }

                        // Sumar final
                        analysisResponse.summary?.let { summary ->
                            snackbarHostState.showSnackbar(
                                "Analiză completă: ${summary.successful_operations}/${summary.total_operations} operații reușite",
                                duration = SnackbarDuration.Long
                            )
                        }
                    } else {
                        snackbarHostState.showSnackbar("Eroare la analiza comprehensivă", duration = SnackbarDuration.Long)
                    }
                }

            } catch (e: Exception) {
                Log.e("CalendarScreen", "Error during sync live", e)
                snackbarHostState.showSnackbar(
                    "Eroare generală: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            } finally {
                isSyncingLive = false
                Log.d("CalendarScreen", "Sync live process completed")
            }
        }
    }

    // Load initial activities when the component is first created
    LaunchedEffect(Unit) {
        loadActivitiesForDateRange(isInitialLoad = true) { isInitialLoading = false }
    }

    // Load more activities on scroll with debounce to avoid request spam/cancellations
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index: Int ->
                val center = Int.MAX_VALUE / 2
                val distance = kotlin.math.abs(index - center)
                Pair(index, distance)
            }
            .distinctUntilChanged()
            .debounce(600)
            .filter { pair: Pair<Int, Int> -> !isInitialLoading && pair.second > 7 }
            .collectLatest { pair: Pair<Int, Int> ->
                Log.d("CalendarScreen", "Scroll triggered load at index ${pair.first} (distance: ${pair.second})")
                loadActivitiesForDateRange(isInitialLoad = false)
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                ModernBottomNavigation(navController = navController)
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B55F6),
                                Color(0xFFA855F7)
                            )
                        )
                    )
            ) {
                if (isInitialLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading calendar activities...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        // Header with updated refresh button
                        CalendarHeader(
                            today,
                            listState,
                            coroutineScope,
                            onRefresh = { performSyncLive() })

                        // Calendar Content
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(Int.MAX_VALUE) { index ->
                                    val calendar = Calendar.getInstance().apply {
                                        time = today.time
                                        add(
                                            Calendar.DAY_OF_MONTH,
                                            (index - Int.MAX_VALUE / 2).toInt()
                                        )
                                    }
                                    val dayDateString = SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.getDefault()
                                    ).format(calendar.time)

                                    // Get all trainings for the day
                                    val trainingsForDay = trainingPlans.filter {
                                        it.date == dayDateString
                                    }

                                    // Get all Strava activities for the day
                                    val stravaActivitiesForDay =
                                        unifiedActivities.filter { activity ->
                                            val activityDate = try {
                                                val utc = SimpleDateFormat(
                                                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                                    Locale.getDefault()
                                                ).apply { timeZone = TimeZone.getTimeZone("UTC") }
                                                val parsed = utc.parse(activity.startDate)
                                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                                    parsed ?: activity.startDate
                                                )
                                            } catch (e: Exception) {
                                                activity.startDate.take(10)
                                            }
                                            activityDate == dayDateString
                                        }

                                    ModernCalendarDayItem(
                                        day = calendar.time,
                                        isToday = isSameDay(calendar.time, today.time),
                                        trainings = trainingsForDay,
                                        stravaActivities = stravaActivitiesForDay,
                                        navController = navController,
                                        stravaViewModel = stravaViewModel,
                                        onDateClick = { training ->
                                            selectedTrainingPlan = training
                                            showDatePicker = true
                                        },
                                        onTrainingClick = { training ->
                                            navController.navigate("loading_training/${training.id}")
                                        },
                                        onMapClick = { activityId ->
                                            selectedActivityId = activityId
                                            showMapDialog = true
                                        },
                                        onDeleteStravaActivity = { activityId ->
                                            coroutineScope.launch {
                                                val success = stravaViewModel.deleteStravaActivity(activityId)
                                                if (success) {
                                                    Log.d("CalendarScreen", "Strava activity $activityId deleted successfully")
                                                    // Refresh activities after deletion
                                                    loadActivitiesForDateRange(false)
                                                } else {
                                                    Log.e("CalendarScreen", "Failed to delete Strava activity $activityId")
                                                }
                                            }
                                        },
                                        onDeleteAppWorkout = { workoutId ->
                                            coroutineScope.launch {
                                                val success = stravaViewModel.deleteAppWorkout(workoutId)
                                                if (success) {
                                                    Log.d("CalendarScreen", "App workout $workoutId deleted successfully")
                                                    // Refresh activities after deletion
                                                    loadActivitiesForDateRange(false)
                                                } else {
                                                    Log.e("CalendarScreen", "Failed to delete app workout $workoutId")
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    if (showDatePicker && selectedTrainingPlan != null) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newDate = datePickerState.selectedDateMillis
                        if (newDate != null) {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val newDateString = sdf.format(Date(newDate))
                            val dateUpdate = TrainingDateUpdate(
                                newDate = newDateString
                            )
                            authViewModel.updateTrainingPlanDate(
                                selectedTrainingPlan!!.id,
                                dateUpdate
                            )
                            showDatePicker = false
                        }
                    }
                ) {
                    Text(text = "Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showMapDialog && selectedActivityId != null) {
        StravaMapDialog(
            activityId = selectedActivityId!!,
            onDismiss = {
                showMapDialog = false
                selectedActivityId = null
            }
        )
    }
}

@Composable
fun StravaMapDialog(
    activityId: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stravaViewModel: StravaViewModel = viewModel(factory = StravaViewModelFactory(context))
    var mapUrl by remember { mutableStateOf<String?>(null) }

    // Load the map view URL when the dialog opens
    LaunchedEffect(activityId) {
        try {
            val mapViewResponse = stravaViewModel.getActivityMapView(activityId)
            mapUrl = mapViewResponse["html_url"]
            Log.d("StravaMapDialog", "Loaded map URL: $mapUrl")
        } catch (e: Exception) {
            Log.e("StravaMapDialog", "Error loading map view", e)
        }
    }

    Popup(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Activity Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDismiss() },
                        tint = Color(0xFF6366F1)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    if (mapUrl != null) {
                        AndroidView(factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                                        return false // Allow WebView to handle URLs
                                    }
                                }
                                loadUrl(mapUrl!!)
                            }
                        })
                    } else {
                        // Loading state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading map...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun CalendarHeader(
    today: Calendar,
    listState: LazyListState,
    coroutineScope: CoroutineScope,
    onRefresh: () -> Unit = {}
) {
    var isRefreshing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Training Calendar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Refresh button
            Card(
                modifier = Modifier
                    .clickable {
                        if (!isRefreshing) {
                            isRefreshing = true
                            onRefresh()
                            coroutineScope.launch {
                                delay(2000) // Reset after 2 seconds
                                isRefreshing = false
                            }
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRefreshing) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF6366F1)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh activities",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Today button
        Card(
            modifier = Modifier
                .clickable {
                    val todayIndex = Int.MAX_VALUE / 2
                    coroutineScope.launch {
                        listState.animateScrollToItem(todayIndex)
                    }
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Today,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Go to Today",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                )
            }
        }
    }
}

@Composable
private fun ModernCalendarDayItem(
    day: java.util.Date,
    isToday: Boolean,
    trainings: List<TrainingPlan>,
    stravaActivities: List<StravaActivity>,
    navController: NavController,
    stravaViewModel: StravaViewModel,
    onDateClick: (TrainingPlan) -> Unit,
    onTrainingClick: (TrainingPlan) -> Unit,
    onMapClick: (Long) -> Unit,
    onDeleteStravaActivity: (Long) -> Unit,
    onDeleteAppWorkout: (Int) -> Unit
) {
    Log.d("CalendarChart", "trainings = $trainings")

    val calendar = Calendar.getInstance().apply { time = day }
    val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) Color(0xFFF0FFF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (trainings.isNotEmpty()) 6.dp else 2.dp
        ),
        border = if (isToday) BorderStroke(2.dp, Color(0xFF6366F1)) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = dayOfWeekFormat.format(day),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = dayFormat.format(day),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) Color(0xFF6366F1) else Color(0xFF1F2937)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = monthFormat.format(day),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9CA3AF)
                    )
                }

                if (isToday) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (trainings.isEmpty() && stravaActivities.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.SelfImprovement,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rest day - No training planned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9CA3AF),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            } else {
                // Display all trainings
                trainings.forEach { training ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrainingClick(training) }
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.FitnessCenter,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = training.workout_name ?: "Training Session",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1F2937)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit date",
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { onDateClick(training) }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = training.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280),
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Non-interactive training chart preview
                            training.steps?.let { steps ->
                                Column {
                                    Text(
                                        text = "Workout Preview",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6B7280),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SimpleTrainingChart(
                                        steps = steps,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Display all Strava activities
                stravaActivities.forEach { stravaActivity ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Navigate to Strava activity (unified) detail screen
                                navController.navigate("strava_activity_detail/${stravaActivity.id?.toLong() ?: 0}")
                            }
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Icon based on type, extra case for WeightTraining
                                when (stravaActivity.type) {
                                    "Run" -> Icon(
                                        imageVector = Icons.Filled.DirectionsRun,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )

                                    "Ride" -> Icon(
                                        imageVector = Icons.Filled.DirectionsBike,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )

                                    "Swim" -> Icon(
                                        imageVector = Icons.Filled.Pool,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )

                                    "Walk" -> Icon(
                                        imageVector = Icons.Filled.SelfImprovement,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )

                                    "WeightTraining" -> Icon(
                                        imageVector = Icons.Filled.FitnessCenter,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )

                                    else -> Icon(
                                        imageVector = Icons.Filled.SportsHandball,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = stravaActivity.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1F2937),
                                    modifier = Modifier.weight(1f)
                                )

                                // Source badge
                                Text(
                                    text = if (stravaActivity.manual == true) "📱 App" else "🟠 Strava",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (stravaActivity.manual == true) Color(0xFF10B981) else Color(
                                        0xFFFFA500
                                    ),
                                    modifier = Modifier.padding(start = 8.dp)
                                )

                                // Map icon only for non-manual (Strava) activities
                                if (stravaActivity.manual != true) {
                                    Icon(
                                        imageVector = Icons.Filled.Map,
                                        contentDescription = "View Map",
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable {
                                                onMapClick(stravaActivity.id?.toLong() ?: 0)
                                            }
                                    )
                                }

                                // Delete icon for all activities
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete Activity",
                                    tint = Color.Red,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            // Handle delete based on activity type
                                            if (stravaActivity.manual == true) {
                                                // Delete app workout using workout ID
                                                onDeleteAppWorkout(stravaActivity.id?.toInt() ?: 0)
                                            } else {
                                                // Delete Strava activity using Strava ID  
                                                onDeleteStravaActivity(stravaActivity.id?.toLong() ?: 0)
                                            }
                                        }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Logged on ${if (stravaActivity.manual == true) "App" else "Strava"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280),
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Relevant metrics based on activity type
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Duration always
                                Column {
                                    Text(
                                        text = formatDuration(stravaActivity.movingTime ?: 0),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF1F2937),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Moving time",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6B7280)
                                    )
                                }

                                // Distance if exists and relevant (not WeightTraining)
                                if (stravaActivity.distance != null && stravaActivity.type != "WeightTraining") {
                                    Column {
                                        Text(
                                            text = formatDistance(stravaActivity.distance),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFF1F2937),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Distance",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }

                                // Power or HR depending on type
                                when (stravaActivity.type) {
                                    "Ride" -> { /* Average power not available in current model */
                                    }

                                    "Run", "Swim", "WeightTraining" -> {
                                        if (stravaActivity.averageHeartrate != null) {
                                            Column {
                                                Text(
                                                    text = "${stravaActivity.averageHeartrate} bpm",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = Color(0xFF1F2937),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Avg HR",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF6B7280)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Route preview only for non-manual with distance (and distance > 0)
                            if (stravaActivity.manual != true && stravaActivity.distance != null && stravaActivity.distance > 0) {
                                RoutePreview(
                                    activityId = stravaActivity.id?.toLong() ?: 0,
                                    stravaViewModel = stravaViewModel,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ... existing code ...
@Composable
private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
// ... existing code ...

@Composable
fun formatDistance(meters: Float?): String {
    if (meters == null) return "N/A"
    return if (meters >= 1000) String.format("%.1f km", meters / 1000f) else "${meters.toInt()} m"
}

@Composable
fun RoutePreview(
    activityId: Long,
    stravaViewModel: StravaViewModel,
    modifier: Modifier = Modifier
) {
    var routePoints by remember { mutableStateOf<List<Offset>?>(null) }
    var mapTileUrl by remember { mutableStateOf<String?>(null) }
    var hasRouteData by remember { mutableStateOf(false) }

    LaunchedEffect(activityId) {
        try {
            val mapViewResponse = stravaViewModel.getActivityMapView(activityId)
            val gpxData = mapViewResponse["gpx_data"] ?: ""
            val points = parseGpxPoints(gpxData)

            // Only proceed if we have actual route data (at least 2 points for a valid route)
            if (points.isNotEmpty() && points.size >= 2) {
                routePoints = points
                hasRouteData = true

                // Calculate map bounds and generate tile URL for real map background
                val minLat = points.minOf { it.y }
                val maxLat = points.maxOf { it.y }
                val minLng = points.minOf { it.x }
                val maxLng = points.maxOf { it.x }

                // Calculate center for the route
                val centerLat = (minLat + maxLat) / 2
                val centerLng = (minLng + maxLng) / 2

                // Generate Static Map URL that covers the entire route bounds
                mapTileUrl = generateStaticMapUrl(
                    centerLat,
                    centerLng,
                    minLat,
                    maxLat,
                    minLng,
                    maxLng,
                    400,
                    200
                )
                Log.d(
                    "RoutePreview",
                    "Generated map tile URL for activity $activityId: $mapTileUrl"
                )
            } else {
                hasRouteData = false
                Log.d("RoutePreview", "No GPS route data available for activity $activityId")
            }
        } catch (e: Exception) {
            Log.e("RoutePreview", "Error loading route data for activity $activityId", e)
            hasRouteData = false
        }
    }

    // Early return - don't render anything if there's no route data
    if (!hasRouteData) {
        return
    }

    // Modern card container with subtle elevation and rounded corners
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background: Real map tiles
            if (mapTileUrl != null) {
                AsyncImage(
                    model = mapTileUrl,
                    contentDescription = "Map background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(android.R.drawable.ic_menu_mapmode),
                    error = painterResource(android.R.drawable.ic_dialog_alert)
                )
            } else {
                // Minimalist loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    Color(0xFFE5E7EB),
                                    RoundedCornerShape(12.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading route...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Overlay: GPS route path with enhanced styling
            if (routePoints != null) {
                val points = routePoints!!
                if (points.size >= 2) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Calculate bounds and maintain aspect ratio
                        val minX = points.minOf { it.x }
                        val maxX = points.maxOf { it.x }
                        val minY = points.minOf { it.y }
                        val maxY = points.maxOf { it.y }

                        val dataWidth = maxX - minX
                        val dataHeight = maxY - minY

                        // Avoid division by zero
                        if (dataWidth == 0f || dataHeight == 0f) {
                            return@Canvas
                        }

                        // Calculate scaling to maintain aspect ratio and fit in canvas
                        val padding = 16f // Increased padding for better visual breathing room
                        val availableWidth = size.width - 2 * padding
                        val availableHeight = size.height - 2 * padding

                        val scaleX = availableWidth / dataWidth
                        val scaleY = availableHeight / dataHeight
                        val scale = minOf(scaleX, scaleY)

                        // Calculate centering offsets
                        val scaledWidth = dataWidth * scale
                        val scaledHeight = dataHeight * scale
                        val offsetX = padding + (availableWidth - scaledWidth) / 2
                        val offsetY = padding + (availableHeight - scaledHeight) / 2

                        // Transform points to canvas coordinates
                        val scaledPoints = points.map { point ->
                            Offset(
                                x = offsetX + (point.x - minX) * scale,
                                y = offsetY + (maxY - point.y) * scale // Flip Y to show north up
                            )
                        }

                        // Draw the route path with modern styling
                        val path = Path().apply {
                            moveTo(scaledPoints[0].x, scaledPoints[0].y)
                            for (i in 1 until scaledPoints.size) {
                                lineTo(scaledPoints[i].x, scaledPoints[i].y)
                            }
                        }

                        // Drop shadow for depth
                        drawPath(
                            path = Path().apply {
                                moveTo(scaledPoints[0].x + 2, scaledPoints[0].y + 2)
                                for (i in 1 until scaledPoints.size) {
                                    lineTo(scaledPoints[i].x + 2, scaledPoints[i].y + 2)
                                }
                            },
                            color = Color.Black.copy(alpha = 0.15f),
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )

                        // Thick white outline for contrast
                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(width = 7f, cap = StrokeCap.Round)
                        )

                        // Main route in vibrant brand color
                        drawPath(
                            path = path,
                            color = Color(0xFF6366F1), // Brand color instead of red
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )

                        // Modern start point with shadow
                        if (scaledPoints.isNotEmpty()) {
                            // Shadow
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.15f),
                                radius = 9f,
                                center = Offset(
                                    scaledPoints.first().x + 1,
                                    scaledPoints.first().y + 1
                                )
                            )
                            // White border
                            drawCircle(
                                color = Color.White,
                                radius = 8f,
                                center = scaledPoints.first()
                            )
                            // Green center
                            drawCircle(
                                color = Color(0xFF10B981),
                                radius = 5f,
                                center = scaledPoints.first()
                            )
                        }

                        // Modern end point with shadow
                        if (scaledPoints.size > 1) {
                            // Shadow
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.15f),
                                radius = 9f,
                                center = Offset(
                                    scaledPoints.last().x + 1,
                                    scaledPoints.last().y + 1
                                )
                            )
                            // White border
                            drawCircle(
                                color = Color.White,
                                radius = 8f,
                                center = scaledPoints.last()
                            )
                            // Red center
                            drawCircle(
                                color = Color(0xFFEF4444),
                                radius = 5f,
                                center = scaledPoints.last()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateZoomLevel(minLat: Float, maxLat: Float, minLng: Float, maxLng: Float): Int {
    // Simple zoom level calculation based on latitude/longitude span
    val latDiff = maxLat - minLat
    val lngDiff = maxLng - minLng
    val maxDiff = maxOf(latDiff, lngDiff)

    return when {
        maxDiff < 0.01f -> 15
        maxDiff < 0.02f -> 14
        maxDiff < 0.05f -> 13
        maxDiff < 0.1f -> 12
        maxDiff < 0.2f -> 11
        maxDiff < 0.5f -> 10
        maxDiff < 1f -> 9
        maxDiff < 2f -> 8
        maxDiff < 5f -> 7
        else -> 6
    }
}

private fun generateStaticMapUrl(
    centerLat: Float,
    centerLng: Float,
    minLat: Float,
    maxLat: Float,
    minLng: Float,
    maxLng: Float,
    width: Int,
    height: Int
): String {
    // Use ESRI ArcGIS for real satellite/street imagery that covers the exact route bounds
    return "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/export?bbox=$minLng,$minLat,$maxLng,$maxLat&bboxSR=4326&imageSR=4326&size=${width},${height}&format=png&f=image"
}

private fun parseGpxPoints(gpxData: String): List<Offset> {
    if (gpxData.isEmpty()) return emptyList()

    try {
        // GPX data comes in format "lat1,lng1;lat2,lng2;lat3,lng3"
        val coordinatePairs = gpxData.split(";")
        if (coordinatePairs.size < 2) return emptyList()

        val points = coordinatePairs.mapNotNull { pair ->
            val coords = pair.split(",")
            if (coords.size == 2) {
                try {
                    val lat = coords[0].toFloat()
                    val lng = coords[1].toFloat()
                    // Return raw coordinates - scaling will be done in Canvas
                    Offset(lng, lat) // lng = x (east-west), lat = y (north-south)
                } catch (e: NumberFormatException) {
                    null
                }
            } else null
        }

        if (points.size < 2) return emptyList()
        return points

    } catch (e: Exception) {
        Log.e("parseGpxPoints", "Error parsing GPX data: $gpxData", e)
        return emptyList()
    }
}

@Composable
fun SimpleTrainingChart(
    steps: List<WorkoutStep>,
    modifier: Modifier = Modifier
) {
    // Calculate total duration and create segments
    val totalDuration = steps.sumOf {
        when (it) {
            is WorkoutStep.SteadyState -> it.duration
            is WorkoutStep.IntervalsT -> it.repeat * (it.on_duration + it.off_duration)
            is WorkoutStep.IntervalsP -> it.repeat * (it.on_duration + it.off_duration)
            is WorkoutStep.Ramp -> it.duration
            is WorkoutStep.FreeRide -> it.duration
            is WorkoutStep.Pyramid -> it.repeat * it.step_duration * 2
            else -> 0
        }
    }.toFloat()

    if (totalDuration == 0f) {
        Box(
            modifier = modifier.background(Color(0xFFF8FAFC), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF)
            )
        }
        return
    }

    // Simple chart with bars
    Row(
        modifier = modifier
            .background(Color(0xFFF8FAFC), RoundedCornerShape(4.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        steps.forEach { step ->
            when (step) {
                is WorkoutStep.SteadyState -> {
                    val weight = step.duration / totalDuration
                    val height = (step.power * 0.8f).coerceIn(0.2f, 1f)
                    SimpleBar(
                        weight = weight,
                        intensity = height,
                        color = Color(0xFF6366F1)
                    )
                }

                is WorkoutStep.IntervalsT -> {
                    repeat(step.repeat) {
                        // Work interval
                        val workWeight = step.on_duration / totalDuration
                        val workHeight = (step.on_power * 0.8f).coerceIn(0.2f, 1f)
                        SimpleBar(
                            weight = workWeight,
                            intensity = workHeight,
                            color = Color(0xFFDC2626)
                        )
                        // Rest interval
                        val restWeight = step.off_duration / totalDuration
                        val restHeight = (step.off_power * 0.8f).coerceIn(0.2f, 1f)
                        SimpleBar(
                            weight = restWeight,
                            intensity = restHeight,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                is WorkoutStep.IntervalsP -> {
                    repeat(step.repeat) {
                        // Work interval
                        val workWeight = step.on_duration / totalDuration
                        val workHeight = (step.on_power * 0.8f).coerceIn(0.2f, 1f)
                        SimpleBar(
                            weight = workWeight,
                            intensity = workHeight,
                            color = Color(0xFFDC2626)
                        )
                        // Rest interval
                        val restWeight = step.off_duration / totalDuration
                        val restHeight = (step.off_power * 0.8f).coerceIn(0.2f, 1f)
                        SimpleBar(
                            weight = restWeight,
                            intensity = restHeight,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                is WorkoutStep.Ramp -> {
                    val weight = step.duration / totalDuration
                    val avgIntensity =
                        ((step.start_power + step.end_power) / 2f * 0.8f).coerceIn(0.2f, 1f)
                    SimpleBar(
                        weight = weight,
                        intensity = avgIntensity,
                        color = Color(0xFF10B981)
                    )
                }

                is WorkoutStep.FreeRide -> {
                    val weight = step.duration / totalDuration
                    val height = 0.5f // Default intensity for FreeRide
                    SimpleBar(
                        weight = weight,
                        intensity = height,
                        color = Color(0xFF10B981)
                    )
                }

                is WorkoutStep.Pyramid -> {
                    val weight = step.repeat * step.step_duration * 2 / totalDuration
                    val height = 0.7f // Default intensity for Pyramid
                    SimpleBar(
                        weight = weight,
                        intensity = height,
                        color = Color(0xFF8B55F7)
                    )
                }

                else -> {
                    // Handle other types with default values
                    SimpleBar(
                        weight = 0.1f,
                        intensity = 0.5f,
                        color = Color(0xFF8B55F7)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.SimpleBar(
    weight: Float,
    intensity: Float,
    color: Color
) {
    Column(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(intensity)
                .background(color, RoundedCornerShape(1.dp))
        )
    }
}

private fun isSameDay(date1: java.util.Date, date2: java.util.Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Preview(showBackground = true)
@Composable
fun InfiniteCalendarPagePreview() {
    MaterialTheme {
        InfiniteCalendarPage(
            navController = NavController(LocalContext.current),
            authViewModel = AuthViewModel(SharedPreferencesMock())
        )
    }
}
