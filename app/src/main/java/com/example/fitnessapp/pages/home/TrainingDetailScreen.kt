package com.example.fitnessapp.pages.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.R
import com.example.fitnessapp.components.TrainingChart
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.ui.theme.WorkoutColors
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.TrainingDetailViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainingDetailScreen(
    training: TrainingPlan,
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: TrainingDetailViewModel = viewModel()
) {
    val metrics by viewModel.calculatedMetrics.observeAsState()
    val ftpEstimate by viewModel.ftpEstimate.observeAsState()
    val cyclingFtp by viewModel.cyclingFtp.observeAsState()
    val runningFtp by viewModel.runningFtp.observeAsState()
    val swimmingPace by viewModel.swimmingPace.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState()
    var error by remember { mutableStateOf<String?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()

    // Add pull-to-refresh handler
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            error = null
            val token = authViewModel.getToken()
            if (!token.isNullOrEmpty()) {
                viewModel.ensureDataLoaded(token, training.workout_type ?: "cycling")
            }
            pullToRefreshState.endRefresh()
        }
    }

    // Fetch FTP estimate when the screen is first displayed
    LaunchedEffect(Unit) {
        try {
            val token = authViewModel.getToken() ?: throw Exception("Not authenticated")
            val workoutType = training.workout_type ?: "cycling"

            // Use the new method to ensure data is loaded only once
            viewModel.ensureDataLoaded(token, workoutType)
        } catch (e: Exception) {
            error = e.message
        }
    }

    // Calculate metrics when the appropriate data is loaded
    LaunchedEffect(isLoading, cyclingFtp, runningFtp, swimmingPace, error) {
        android.util.Log.d("TrainingDetailScreen", "=== CALCULATION TRIGGER DEBUG ===")
        android.util.Log.d("TrainingDetailScreen", "isLoading: $isLoading")
        android.util.Log.d("TrainingDetailScreen", "error: $error")
        android.util.Log.d("TrainingDetailScreen", "cyclingFtp: $cyclingFtp")
        android.util.Log.d("TrainingDetailScreen", "runningFtp: $runningFtp")
        android.util.Log.d("TrainingDetailScreen", "swimmingPace: $swimmingPace")
        android.util.Log.d("TrainingDetailScreen", "workout_type: ${training.workout_type}")

        if (isLoading == false && error == null) {
            val workoutType = training.workout_type?.lowercase() ?: "cycling"

            // Wait for the appropriate data to be loaded before calculating
            val shouldCalculate = when (workoutType) {
                "cycling" -> cyclingFtp != null
                "running" -> runningFtp != null
                "swimming" -> swimmingPace != null
                else -> cyclingFtp != null
            }

            android.util.Log.d("TrainingDetailScreen", "workoutType: $workoutType")
            android.util.Log.d("TrainingDetailScreen", "shouldCalculate: $shouldCalculate")

            if (shouldCalculate) {
                // Use appropriate FTP value or default
                val ftpToUse = when (workoutType) {
                    "cycling" -> cyclingFtp?.cyclingFtp?.toFloat() ?: 200f
                    "running" -> paceToSpeed(runningFtp?.runningFtp ?: "5:00")
                    "swimming" -> 200f // Swimming doesn't use FTP directly
                    else -> cyclingFtp?.cyclingFtp?.toFloat() ?: 200f
                }

                android.util.Log.d("TrainingDetailScreen", "ftpToUse: $ftpToUse")
                android.util.Log.d("TrainingDetailScreen", "Calling calculateTrainingMetrics...")

                viewModel.calculateTrainingMetrics(training, ftpToUse)
            } else {
                android.util.Log.d("TrainingDetailScreen", "Not calculating - waiting for data")
            }
        } else {
            android.util.Log.d(
                "TrainingDetailScreen",
                "Not calculating - isLoading: $isLoading, error: $error"
            )
        }
        android.util.Log.d("TrainingDetailScreen", "===================================")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFFA855F7)
                    )
                )
            )
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        navController.navigate("calendar_screen") {
                            popUpTo("calendar_screen") { inclusive = true }
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Navigate back to calendar"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Workout type icon
                    Icon(
                        imageVector = when (training.workout_type?.lowercase()) {
                            "cycling" -> Icons.Default.DirectionsBike
                            "running" -> Icons.Default.DirectionsRun
                            "swimming" -> Icons.Default.Pool
                            else -> Icons.Default.Speed
                        },
                        contentDescription = "Workout type: ${training.workout_type}",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = training.workout_name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Placeholder for symmetry
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Content Card
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                when {
                    isLoading == true -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Animated loading dots
                            val infiniteTransition = rememberInfiniteTransition(label = "loading")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeat(3) { index ->
                                    val dotScale by infiniteTransition.animateFloat(
                                        initialValue = 0.5f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(
                                                durationMillis = 600,
                                                delayMillis = index * 200,
                                                easing = LinearEasing
                                            ),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "dot$index"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .graphicsLayer(scaleX = dotScale, scaleY = dotScale)
                                            .background(
                                                color = Color(0xFF6366F1),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.calculating_training_metrics),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                    error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = WorkoutColors.errorRed.copy(alpha = 0.1f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = WorkoutColors.errorRed,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.error_prefix, error ?: "Unknown error"),
                                        color = WorkoutColors.errorRed,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            error = null
                                            val token = authViewModel.getToken()
                                            if (!token.isNullOrEmpty()) {
                                                // Retry with the appropriate method based on workout type
                                                when (training.workout_type?.lowercase()) {
                                                    "running" -> {
                                                        viewModel.fetchRunningFtp(token)
                                                    }

                                                    "swimming" -> {
                                                        viewModel.fetchSwimmingPace(token)
                                                    }

                                                    "cycling", null -> {
                                                        viewModel.fetchCyclingFtp(token)
                                                    }

                                                    else -> {
                                                        viewModel.fetchCyclingFtp(token) // Default to cycling
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WorkoutColors.primaryPurple
                                        ),
                                        modifier = Modifier.semantics {
                                            contentDescription = "Retry loading training data"
                                        }
                                    ) {
                                        Text(
                                            text = stringResource(R.string.retry_button),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                            // Training Type Card
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(stringResource(R.string.test_tag_training_type_card)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = WorkoutColors.getBackgroundColor(training.workout_type)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = stringResource(R.string.training_type),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = WorkoutColors.textSecondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = training.workout_type?.replaceFirstChar { it.uppercase() }
                                                        ?: stringResource(R.string.unknown_workout),
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = WorkoutColors.getTextColor(training.workout_type),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Display the training chart if steps are available
                            training.steps?.let { steps ->
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag(stringResource(R.string.test_tag_workout_structure_card)),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.workout_structure),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = WorkoutColors.defaultText,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )
                                            TrainingChart(
                                                steps = steps,
                                                workoutType = training.workout_type ?: "cycling",
                                                cyclingFtp = cyclingFtp?.cyclingFtp?.toFloat(),
                                                runningFtp = runningFtp?.runningFtp?.let {
                                                    paceToSpeed(
                                                        it
                                                    )
                                                },
                                                swimmingPace = swimmingPace?.pace100m
                                            )

                                            // Zone Distribution Breakdown
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = stringResource(R.string.zone_distribution),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = WorkoutColors.defaultText
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            ZoneDistributionBars(
                                                steps = steps, 
                                                ftp = cyclingFtp?.cyclingFtp?.toFloat() ?: 200f
                                            )
                                        }
                                    }
                                }
                            }

                            // Empty state for missing workout structure
                            if (training.steps == null) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag(stringResource(R.string.test_tag_empty_state_card)),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = null,
                                                tint = Color(0xFF9CA3AF),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = stringResource(R.string.no_steps_data),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = WorkoutColors.defaultText
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = stringResource(R.string.no_steps_data_message),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = WorkoutColors.textSecondary,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // Display training metrics
                            metrics?.let { trainingMetrics ->
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag(stringResource(R.string.test_tag_metrics_card)),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp)
                                        ) {
                                            Crossfade(
                                                targetState = trainingMetrics, 
                                                animationSpec = tween(300),
                                                label = "metrics_crossfade"
                                            ) { animatedMetrics ->
                                                if (animatedMetrics != null) {
                                                    FlowRow(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceEvenly
                                                    ) {
                                                        ModernMetricItem(
                                                            label = stringResource(R.string.tss_label),
                                                            value = String.format("%.1f", animatedMetrics.tss),
                                                            icon = Icons.Default.Speed
                                                        )
                                                        ModernMetricItem(
                                                            label = stringResource(R.string.duration_label),
                                                            value = formatDuration(animatedMetrics.duration),
                                                            icon = Icons.Default.AccessTime
                                                        )
                                                        ModernMetricItem(
                                                            label = stringResource(R.string.calories_label),
                                                            value = String.format("%.0f", animatedMetrics.calories),
                                                            icon = Icons.Default.LocalFireDepartment
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = "Loading metrics...", 
                                                        color = Color.Gray,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }

                                            // Display sport-specific data
                                            when (training.workout_type?.lowercase()) {
                                                "cycling" -> {
                                                    cyclingFtp?.let { cycling ->
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        SportSpecificCard(
                                                            title = stringResource(R.string.cycling_ftp),
                                                            workoutType = "cycling",
                                                            leftLabel = stringResource(R.string.ftp_label),
                                                            leftValue = "${cycling.cyclingFtp} ${stringResource(R.string.watts_unit)}",
                                                            rightLabel = stringResource(R.string.fthr_label),
                                                            rightValue = "${cycling.fthrCycling} ${stringResource(R.string.bpm_unit)}",
                                                            modifier = Modifier.testTag(stringResource(R.string.test_tag_sport_specific_card))
                                                        )
                                                    }
                                                }

                                                "running" -> {
                                                    runningFtp?.let { running ->
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        SportSpecificCard(
                                                            title = stringResource(R.string.running_data),
                                                            workoutType = "running",
                                                            leftLabel = stringResource(R.string.running_pace_label),
                                                            leftValue = formatRunningPace(running.runningFtp),
                                                            rightLabel = stringResource(R.string.fthr_label),
                                                            rightValue = "${running.fthrRunning} ${stringResource(R.string.bpm_unit)}",
                                                            modifier = Modifier.testTag(stringResource(R.string.test_tag_sport_specific_card))
                                                        )
                                                    }
                                                }

                                                "swimming" -> {
                                                    swimmingPace?.let { swimming ->
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        SportSpecificCard(
                                                            title = stringResource(R.string.swimming_data),
                                                            workoutType = "swimming",
                                                            leftLabel = stringResource(R.string.pace_100m_label),
                                                            leftValue = swimming.pace100m,
                                                            rightLabel = stringResource(R.string.fthr_swimming_label),
                                                            rightValue = "${swimming.fthrSwimming} ${stringResource(R.string.bpm_unit)}",
                                                            modifier = Modifier.testTag(stringResource(R.string.test_tag_sport_specific_card))
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    // Show default message for unknown workout types
                                                    if (isLoading == false && error == null) {
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Card(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = WorkoutColors.defaultBg
                                                            ),
                                                            elevation = CardDefaults.cardElevation(
                                                                defaultElevation = 2.dp
                                                            )
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.padding(16.dp)
                                                            ) {
                                                                Text(
                                                                    text = stringResource(R.string.training_information),
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = WorkoutColors.defaultText
                                                                )
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                Text(
                                                                    text = stringResource(R.string.using_default_values),
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = WorkoutColors.textSecondary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Display the complete description
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(stringResource(R.string.test_tag_description_card)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.description),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = WorkoutColors.defaultText,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        // Scrollable description for long text
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Text(
                                                text = training.description,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color(0xFF374151),
                                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                                            )
                                        }
                                    }
                                }
                            }

                            // Strava Integration Teaser
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(stringResource(R.string.test_tag_strava_sync_card)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_strava),
                                                contentDescription = stringResource(R.string.strava_logo_description),
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.sync_to_strava),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color(0xFF374151)
                                            )
                                        }
                                        Button(
                                            onClick = { /* Handle Strava sync logic */ },
                                            colors = ButtonDefaults.buttonColors(containerColor = WorkoutColors.primaryPurple),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.semantics {
                                                contentDescription = "Sync workout to Strava"
                                            }
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.sync_button), color = Color.White)
                                        }
                                    }
                                }
                            }

                            // Start workout button
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { 
                                        navController.navigate("workout_execution/${training.id}")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag(stringResource(R.string.test_tag_start_workout_button))
                                        .semantics {
                                            contentDescription = "Start ${training.workout_name} workout"
                                        },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WorkoutColors.primaryPurple
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.start_workout),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        }
                    }
                }

            }
        }

        // Add the pull-to-refresh container at the top
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun ModernMetricItem(label: String, value: String, icon: ImageVector? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6366F1)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SportSpecificCard(
    title: String,
    workoutType: String,
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WorkoutColors.getBackgroundColor(workoutType)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp) // Enhanced shadow
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.defaultText
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = leftLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = WorkoutColors.textSecondary
                    )
                    Text(
                        text = leftValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutColors.getTextColor(workoutType)
                    )
                }
                Column {
                    Text(
                        text = rightLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = WorkoutColors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    // Enhanced FTHR highlighting with background
                    Box(
                        modifier = Modifier
                            .background(
                                color = WorkoutColors.getTextColor(workoutType).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = rightValue,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold, // Extra bold for FTHR emphasis
                            color = WorkoutColors.getTextColor(workoutType)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> String.format("%dh %dm", hours, minutes)
        else -> String.format("%dm", minutes)
    }
}

private fun paceToSpeed(pace: String): Float {
    return try {
        if (pace.contains(":")) {
            // Format like "5:00" (min:sec per km)
            val paceParts = pace.split(":")
            val minutes = paceParts[0].toInt()
            val seconds = paceParts.getOrElse(1) { "0" }.toInt()
            val totalSeconds = minutes * 60 + seconds
            1000f / totalSeconds // Convert to m/s
        } else {
            // Format like "4.98" (decimal minutes per km)
            val decimalMinutes = pace.toFloat()
            val totalSeconds = decimalMinutes * 60f // Convert to seconds per km
            1000f / totalSeconds // Convert to m/s
        }
    } catch (e: Exception) {
        4.0f // Default fallback
    }
}

private fun formatRunningPace(pace: String): String {
    return try {
        if (pace.contains(":")) {
            // Already in pace format like "5:00"
            "$pace/km"
        } else {
            // It's decimal minutes per km (e.g., 4.98 = 4 minutes 58.8 seconds)
            val decimalMinutes = pace.toFloat()
            val minutes = decimalMinutes.toInt()
            val seconds = ((decimalMinutes - minutes) * 60).toInt()
            String.format("%d:%02d/km", minutes, seconds)
        }
    } catch (e: Exception) {
        "5:00/km" // Default fallback
    }
}

// Zone Distribution Bars composable for showing time spent in each power/HR zone
@Composable
private fun ZoneDistributionBars(steps: List<com.example.fitnessapp.model.WorkoutStep>, ftp: Float) {
    // Placeholder calculation: Assume 5 zones, distribute time (implement properly in ViewModel)
    val zones = listOf(20f, 30f, 25f, 15f, 10f) // % time in zones 1-5

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .semantics {
                contentDescription = "Zone distribution showing time spent in each power or heart rate zone"
            },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        zones.forEachIndexed { index, percent ->
            Box(
                modifier = Modifier
                    .weight(percent)
                    .fillMaxHeight()
                    .background(getZoneColor(index + 1))
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        (1..5).forEach { zone ->
            Text(
                text = "Z$zone", 
                style = MaterialTheme.typography.bodySmall, 
                color = Color.Gray
            )
        }
    }
}

// Helper function for zone colors
private fun getZoneColor(zone: Int): Color {
    return when (zone) {
        1 -> Color(0xFF6EE7B7) // Light green
        2 -> Color(0xFF34D399) // Green
        3 -> Color(0xFF10B981) // Medium green
        4 -> Color(0xFF059669) // Dark green
        else -> Color(0xFF047857) // Darkest green
    }
}

@Preview(showBackground = true)
@Composable
fun TrainingDetailScreenPreview() {
    TrainingDetailScreen(
        training = TrainingPlan(
            id = 1,
            user_id = 1,
            date = "2024-06-01",
            workout_name = "Test Workout",
            duration = "01:00:00",
            intensity = "Medium",
            description = "Test description",
            workout_type = "cycling",
            zwo_path = null,
            stepsJson = null
        ),
        navController = rememberNavController(),
        authViewModel = AuthViewModel(SharedPreferencesMock())
    )
}
