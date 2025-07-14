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
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsHandball
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    // Maintain state for database activities
    var isInitialLoading by remember { mutableStateOf(true) }
    var databaseActivities by remember { mutableStateOf<List<StravaActivity>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Sync live state
    var isSyncingLive by remember { mutableStateOf(false) }
    var syncPopupVisible by remember { mutableStateOf(false) }
    var syncCurrentActivity by remember { mutableStateOf<String?>(null) }
    var syncActivitiesCount by remember { mutableStateOf(0) }
    var syncProgress by remember { mutableStateOf(0f) }
    var syncError by remember { mutableStateOf<String?>(null) }

    Log.d("CalendarScreen", "Strava Activities: $stravaActivities")
    Log.d("CalendarScreen", "Database Activities: $databaseActivities")

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

    // Function to calculate date range based on visible items and fetch activities
    fun loadActivitiesForDateRange(isInitialLoad: Boolean, onFinished: () -> Unit = {}) {
        coroutineScope.launch {
            try {
                val startCalendar: Calendar
                val endCalendar: Calendar
                val bufferDays = if (isInitialLoad) 7 else 30 // Smaller initial load

                if (isInitialLoad) {
                    // For the initial load, just load a week around today
                    startCalendar = Calendar.getInstance().apply {
                        time = today.time
                        add(Calendar.DAY_OF_MONTH, -bufferDays)
                    }
                    endCalendar = Calendar.getInstance().apply {
                        time = today.time
                        add(Calendar.DAY_OF_MONTH, bufferDays)
                    }
                } else {
                    // For subsequent loads, use the listState to determine the visible range
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
                    "Loading activities from $startDate to $endDate (isInitial: $isInitialLoad)"
                )

                // Only fetch if we don't already have activities for this range
                if (isInitialLoad || databaseActivities.isEmpty()) {
                    val activities = stravaViewModel.getActivitiesFromDatabase(startDate, endDate)
                    databaseActivities = activities
                    Log.d("CalendarScreen", "Loaded ${activities.size} activities from database")
                } else {
                    Log.d(
                        "CalendarScreen",
                        "Skipping load - already have ${databaseActivities.size} activities"
                    )
                }
            } catch (e: Exception) {
                Log.e("CalendarScreen", "Error loading activities for date range", e)
            } finally {
                onFinished()
            }
        }
    }


    // Function to perform sync live
    fun performSyncLive() {
        if (isSyncingLive) return

        isSyncingLive = true
        syncPopupVisible = true
        syncCurrentActivity = null
        syncActivitiesCount = 0
        syncProgress = 0f
        syncError = null

        coroutineScope.launch {
            try {
                val jwtToken = authViewModel.getToken()
                if (jwtToken.isNullOrEmpty()) {
                    syncError = "Token de autentificare lipsă"
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
                        withContext(Dispatchers.Main) {
                            syncCurrentActivity =
                                "Reconectare... (încercarea $currentRetry/$maxRetries)"
                        }
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
                                            withContext(Dispatchers.Main) {
                                                syncCurrentActivity = "Sincronizare completă!"
                                                syncProgress = 1.0f
                                                syncActivitiesCount =
                                                    totalActivitiesSynced + activitiesCount

                                                // Hide popup after delay
                                                delay(2000)
                                                syncPopupVisible = false
                                            }

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
                                            withContext(Dispatchers.Main) {
                                                syncError = errorMessage
                                                syncCurrentActivity = "Eroare: $errorMessage"
                                            }
                                            throw Exception(errorMessage)
                                        }

                                        // Handle individual activity sync events
                                        if (eventData.has("name") && eventData.has("start_date")) {
                                            activitiesCount++
                                            val activityName = eventData.get("name").asString
                                            val startDate = eventData.get("start_date").asString

                                            Log.d(
                                                "CalendarScreen",
                                                "Activity synced: $activityName ($startDate)"
                                            )

                                            withContext(Dispatchers.Main) {
                                                syncCurrentActivity = activityName
                                                syncActivitiesCount =
                                                    totalActivitiesSynced + activitiesCount

                                                // Update progress based on activities synced
                                                syncProgress = when {
                                                    activitiesCount <= 5 -> 0.2f + (activitiesCount * 0.1f) // 0.2 to 0.7 for first 5
                                                    activitiesCount <= 15 -> 0.7f + ((activitiesCount - 5) * 0.02f) // 0.7 to 0.9 for next 10
                                                    else -> 0.9f + (0.05f / maxOf(
                                                        1,
                                                        activitiesCount - 15
                                                    )) // 0.9+ for remaining
                                                }.coerceAtMost(0.95f)
                                            }

                                            delay(150) // Small delay to show each activity
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

                                withContext(Dispatchers.Main) {
                                    syncCurrentActivity = "Conexiunea s-a întrerupt. Reconectare..."
                                    syncProgress = 0.3f + (currentRetry * 0.1f)
                                }
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
                                withContext(Dispatchers.Main) {
                                    syncCurrentActivity = "Problemă de conexiune. Reconectare..."
                                }
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
                    withContext(Dispatchers.Main) {
                        syncError = message
                        syncCurrentActivity = message
                    }
                }

            } catch (e: Exception) {
                Log.e("CalendarScreen", "Error during sync live", e)
                withContext(Dispatchers.Main) {
                    syncError = e.message ?: "Eroare necunoscută"
                    syncCurrentActivity = "Eroare: $syncError"

                    // Hide popup after error delay
                    delay(3000)
                    syncPopupVisible = false
                }
            } finally {
                isSyncingLive = false
                Log.d("CalendarScreen", "Sync live process completed")
            }
        }
    }

    // Load initial activities when the component is first created
    LaunchedEffect(Unit) {
        loadActivitiesForDateRange(
            isInitialLoad = true,
            onFinished = { isInitialLoading = false }
        )
    }

    // Load more activities when scroll position changes significantly
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (!isInitialLoading) {
            // Add throttling - only load if we've scrolled significantly
            val currentIndex = listState.firstVisibleItemIndex
            val center = Int.MAX_VALUE / 2
            val distanceFromCenter = kotlin.math.abs(currentIndex - center)

            // Only load more if we're far from what we've already loaded
            if (distanceFromCenter > 50) { // Only load if scrolled more than 50 items
                Log.d(
                    "CalendarScreen",
                    "Scroll triggered load at index $currentIndex (distance: $distanceFromCenter)"
                )
                loadActivitiesForDateRange(isInitialLoad = false)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                ModernBottomNavigation(navController = navController)
            },
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
                                    val trainingForDay = trainingPlans.find {
                                        it.date == SimpleDateFormat(
                                            "yyyy-MM-dd",
                                            Locale.getDefault()
                                        ).format(calendar.time)
                                    }

                                    // Combined search: first from live activities, then from database activities
                                    val dayDateString = SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.getDefault()
                                    ).format(calendar.time)

                                    val stravaForDay = stravaActivities.find { activity ->
                                        val activityDate = try {
                                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                .format(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                                        Locale.getDefault()
                                                    )
                                                        .parse(activity.startDate)
                                                        ?: activity.startDate
                                                )
                                        } catch (e: Exception) {
                                            // Fallback: try to extract date directly if it's already in the right format
                                            activity.startDate.take(10)
                                        }
                                        activityDate == dayDateString
                                    } ?: databaseActivities.find { activity ->
                                        val activityDate = try {
                                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                .format(
                                                    SimpleDateFormat(
                                                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                                        Locale.getDefault()
                                                    )
                                                        .parse(activity.startDate)
                                                        ?: activity.startDate
                                                )
                                        } catch (e: Exception) {
                                            // Fallback: try to extract date directly if it's already in the right format
                                            activity.startDate.take(10)
                                        }
                                        activityDate == dayDateString
                                    }

                                    ModernCalendarDayItem(
                                        day = calendar.time,
                                        isToday = isSameDay(calendar.time, today.time),
                                        training = trainingForDay,
                                        stravaActivity = stravaForDay,
                                        navController = navController,
                                        stravaViewModel = stravaViewModel,
                                        onDateClick = {
                                            if (trainingForDay != null) {
                                                selectedTrainingPlan = trainingForDay
                                                showDatePicker = true
                                            }
                                        },
                                        onTrainingClick = {
                                            if (trainingForDay != null) {
                                                navController.navigate("loading_training/${trainingForDay.id}")
                                            }
                                        },
                                        onMapClick = { activityId ->
                                            selectedActivityId = activityId
                                            showMapDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sync Live Popup Overlay
        if (syncPopupVisible) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, 100),
                properties = PopupProperties(focusable = false, dismissOnBackPress = false)
            ) {
                Card(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .widthIn(min = 320.dp, max = 400.dp)
                    ) {
                        // Header with title and close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (syncError != null) "Eroare sincronizare" else "Sincronizare Strava",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (syncError != null) Color(0xFFEF4444) else Color(
                                    0xFF1F2937
                                )
                            )

                            IconButton(
                                onClick = {
                                    syncPopupVisible = false
                                    // Don't cancel the sync, just hide the popup
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Minimize",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress indicator and status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (syncError != null) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_dialog_alert),
                                    contentDescription = "Error",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF6366F1),
                                    progress = { if (syncProgress > 0) syncProgress else 0f }
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                // Current activity or status
                                if (syncCurrentActivity != null) {
                                    Text(
                                        text = syncCurrentActivity!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (syncError != null) Color(0xFFEF4444) else Color(
                                            0xFF1F2937
                                        ),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                // Progress details
                                if (syncActivitiesCount > 0 && syncError == null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$syncActivitiesCount activități sincronizate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6366F1),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Progress percentage
                                if (syncProgress > 0 && syncError == null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${(syncProgress * 100).toInt()}% complet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }

                        // Progress bar (visual indicator)
                        if (syncError == null && syncProgress > 0) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(
                                        Color(0xFFE5E7EB),
                                        RoundedCornerShape(2.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(syncProgress.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(
                                            Color(0xFF6366F1),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }

                        // Status message for completed sync
                        if (syncProgress >= 1.0f && syncError == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check_circle),
                                    contentDescription = "Success",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sincronizare completă cu succes!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Help text
                        if (isSyncingLive && syncError == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Poți continua să navighezi prin aplicație în timp ce sincronizarea rulează în fundal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
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
    training: TrainingPlan?,
    stravaActivity: StravaActivity?,
    navController: NavController,
    stravaViewModel: StravaViewModel,
    onDateClick: () -> Unit,
    onTrainingClick: () -> Unit,
    onMapClick: (Long) -> Unit
) {
    Log.d("CalendarChart", "steps = ${training?.steps}")

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
            defaultElevation = if (training != null) 6.dp else 2.dp
        ),
        border = if (isToday) BorderStroke(2.dp, Color(0xFF6366F1)) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (training != null) {
                            Modifier.clickable { onDateClick() }
                        } else {
                            Modifier
                        }
                    )
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

                if (training != null) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit date",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
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

            // Training Content - clickable for navigation
            if (training != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrainingClick() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
            } else if (stravaActivity != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Navigate to Strava activity detail screen
                            navController.navigate("strava_activity_detail/${stravaActivity.id?.toLong() ?: 0}")
                        },
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

                            // Map view icon
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Logged on Strava",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Display Strava activity details with proper distance formatting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Distance (convert from meters to km) 
                            val distanceKm = stravaActivity.distance?.div(1000.0) ?: 0.0
                            Column {
                                Text(
                                    text = String.format("%.1f km", distanceKm),
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

                            // Duration (convert from seconds to hr:min:sec format)
                            val totalSeconds = stravaActivity.movingTime ?: 0
                            val hours = totalSeconds / 3600
                            val minutes = (totalSeconds % 3600) / 60
                            val seconds = totalSeconds % 60

                            Column {
                                Text(
                                    text = if (hours > 0) {
                                        String.format("%d:%02d:%02d", hours, minutes, seconds)
                                    } else {
                                        String.format("%d:%02d", minutes, seconds)
                                    },
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
                        }

                        // Modern minimalist route preview with enhanced design
                        RoutePreview(
                            activityId = stravaActivity.id?.toLong() ?: 0,
                            stravaViewModel = stravaViewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp) // Slightly taller for better proportions
                                .padding(vertical = 12.dp) // More spacing
                        )
                    }
                }
            } else {
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
            }
        }
    }
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
