package com.example.fitnessapp.pages.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fitnessapp.model.*
import com.example.fitnessapp.viewmodel.TrainerViewModel
import com.example.fitnessapp.viewmodel.TrainerViewModelFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.ui.theme.FitnessAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExecutionScreen(
    trainingId: Int,
    training: TrainingPlan,
    navController: NavController
) {
    Log.d("WorkoutExecution", "[DEBUG_LOG] WorkoutExecutionScreen started with trainingId: $trainingId, training.id: ${training.id}, training.workout_name: ${training.workout_name}")

    val context = LocalContext.current
    val trainerViewModel: TrainerViewModel = viewModel(
        factory = TrainerViewModelFactory(context)
    )
    val currentSession by trainerViewModel.currentSession.observeAsState()
    val realTimeData by trainerViewModel.realTimeData.observeAsState(RealTimeData())
    val connectionState by trainerViewModel.connectionState.observeAsState(ConnectionState.DISCONNECTED)
    val connectedDevices by trainerViewModel.connectedDevices.observeAsState(emptyList())
    val availableDevices by trainerViewModel.availableDevices.observeAsState(emptyList())
    val ergMode by trainerViewModel.ergMode.observeAsState(false)

    // Snackbar state for user feedback
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Launcher pentru cererea permisiunilor Bluetooth
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d("WorkoutExecution", "Bluetooth permissions granted")
            // Acum poți începe scanarea
            trainerViewModel.scanForDevices()
        } else {
            Log.e("WorkoutExecution", "Bluetooth permissions denied")
            // Poți arăta un mesaj utilizatorului despre necesitatea permisiunilor
        }
    }

    // Funcție pentru verificarea și cererea permisiunilor
    fun requestBluetoothPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.d("WorkoutExecution", "Requesting missing permissions: ${missingPermissions.joinToString()}")
            bluetoothPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            // Permisiunile sunt deja acordate
            Log.d("WorkoutExecution", "All Bluetooth permissions already granted")
            trainerViewModel.scanForDevices()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Execution") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp), // Add padding to prevent overlap with bottom controls
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header cu informații despre antrenament
                WorkoutHeader(training)

                // Status conexiune trainer
                TrainerConnectionCard(
                    connectionState = connectionState,
                    connectedDevices = connectedDevices.filter { it.type != TrainerType.HEART_RATE_MONITOR },
                    availableDevices = availableDevices.filter { it.type != TrainerType.HEART_RATE_MONITOR },
                    trainerViewModel = trainerViewModel,
                    onScanRequest = ::requestBluetoothPermissions
                )

                // Status conexiune Heart Rate
                HeartRateConnectionCard(
                    connectionState = connectionState,
                    connectedDevices = connectedDevices.filter { it.type == TrainerType.HEART_RATE_MONITOR },
                    availableDevices = availableDevices.filter { it.type == TrainerType.HEART_RATE_MONITOR },
                    trainerViewModel = trainerViewModel,
                    onScanRequest = ::requestBluetoothPermissions
                )

                // ERG Status Card (doar dacă e conectat un trainer)
                if (connectedDevices.any { it.type != TrainerType.HEART_RATE_MONITOR && it.isConnected }) {
                    ErgStatusCard(ergMode, trainerViewModel.getCurrentTargetPower(), realTimeData.power, trainerViewModel)
                }

                // Pasul curent din antrenament
                if (currentSession != null) {
                    CurrentWorkoutStepCard(currentSession!!)

                    // Date în timp real
                    RealTimeMetricsCard(realTimeData)
                }

                // Conditional start button when no session
                if (currentSession == null) {
                    val isTrainerConnected = connectedDevices.any { it.type != TrainerType.HEART_RATE_MONITOR && it.isConnected }
                    if (isTrainerConnected && connectionState == ConnectionState.CONNECTED) {
                        Button(
                            onClick = { 
                                Log.d("WorkoutExecution", "[DEBUG_LOG] Starting workout from button click - training.id: ${training.id}")
                                val sharedPreferences = context.getSharedPreferences("fitness_app_prefs", android.content.Context.MODE_PRIVATE)
                                val token = sharedPreferences.getString("jwt_token", null)
                                trainerViewModel.startWorkout(training, token)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Workout",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFFE5E7EB)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connect trainer first",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom controls when session is active
            if (currentSession != null) {
                WorkoutControlsCard(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    session = currentSession!!,
                    onStart = { 
                        Log.d("WorkoutExecution", "[DEBUG_LOG] Starting workout from controls - training.id: ${training.id}")
                        val sharedPreferences = context.getSharedPreferences("fitness_app_prefs", android.content.Context.MODE_PRIVATE)
                        val token = sharedPreferences.getString("jwt_token", null)
                        trainerViewModel.startWorkout(training, token)
                    },
                    onPause = { trainerViewModel.pauseWorkout() },
                    onResume = { trainerViewModel.resumeWorkout() },
                    onStop = { 
                        trainerViewModel.stopWorkout(
                            onMessage = { message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            },
                            onSaveCompleted = { 
                                navController.navigateUp() // Navighează DOAR după ce salvarea s-a terminat
                            }
                        )
                    },
                    onSkip = { trainerViewModel.skipToNextStep() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WorkoutExecutionScreenPreview() {
    val navController = rememberNavController()
    val sampleTraining = TrainingPlan(
        id = 1,
        user_id = 1,
        date = "2024-01-01",
        workout_name = "Intervals Session",
        duration = "01:00:00",
        intensity = "Hard",
        description = "Sample workout description",
        workout_type = "Cycling",
        zwo_path = null,
        stepsJson = null
    )
    FitnessAppTheme {
        WorkoutExecutionScreen(trainingId = 1, training = sampleTraining, navController = navController)
    }
}

@Composable
private fun WorkoutHeader(training: TrainingPlan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = training.workout_name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Duration: ${training.duration} | Intensity: ${training.intensity}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun TrainerConnectionCard(
    connectionState: ConnectionState,
    connectedDevices: List<TrainerDevice>,
    availableDevices: List<TrainerDevice>,
    trainerViewModel: TrainerViewModel,
    onScanRequest: () -> Unit
) {
    var showDeviceDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trainer Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                when (connectionState) {
                    ConnectionState.CONNECTED -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = Color(0xFF10B981)
                        )
                    }
                    ConnectionState.SCANNING -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                    ConnectionState.CONNECTING -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                    else -> {
                        Button(
                            onClick = { 
                                onScanRequest()
                                showDeviceDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1)
                            )
                        ) {
                            Text("Scan")
                        }
                    }
                }
            }

            if (connectedDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                connectedDevices.forEach { device ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓ ${device.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF10B981)
                        )
                        TextButton(
                            onClick = { trainerViewModel.disconnectDevice(device.id) }
                        ) {
                            Text("Disconnect", color = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }

    // Device Selection Dialog
    if (showDeviceDialog) {
        // Timeout pentru dialog după 45 secunde
        LaunchedEffect(showDeviceDialog) {
            if (showDeviceDialog) {
                delay(45000)
                if (connectionState == ConnectionState.SCANNING || connectionState == ConnectionState.CONNECTING) {
                    Log.w("WorkoutExecution", "Dialog timeout - closing")
                    showDeviceDialog = false
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = { Text("Select Trainer Device") },
            text = {
                Column {
                    if (connectionState == ConnectionState.SCANNING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning for devices...")
                        }
                    } else if (connectionState == ConnectionState.CONNECTING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting to device...")
                        }
                    } else {
                        if (availableDevices.isEmpty()) {
                            Text(
                                text = "No devices found. Try scanning again.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        } else {
                            availableDevices.forEach { device ->
                                TextButton(
                                    onClick = {
                                        trainerViewModel.connectToDevice(device)
                                        showDeviceDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${device.name} (${device.type})")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun HeartRateConnectionCard(
    connectionState: ConnectionState,
    connectedDevices: List<TrainerDevice>,
    availableDevices: List<TrainerDevice>,
    trainerViewModel: TrainerViewModel,
    onScanRequest: () -> Unit
) {
    var showDeviceDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Heart Rate Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                when (connectionState) {
                    ConnectionState.CONNECTED -> {
                        if (connectedDevices.isNotEmpty()) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Connected",
                                tint = Color(0xFF10B981)
                            )
                        } else {
                            Button(
                                onClick = { 
                                    onScanRequest()
                                    showDeviceDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1)
                                )
                            ) {
                                Text("Scan")
                            }
                        }
                    }
                    ConnectionState.SCANNING -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                    ConnectionState.CONNECTING -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                    else -> {
                        Button(
                            onClick = { 
                                onScanRequest()
                                showDeviceDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1)
                            )
                        ) {
                            Text("Scan")
                        }
                    }
                }
            }

            if (connectedDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                connectedDevices.forEach { device ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓ ${device.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF10B981)
                        )
                        TextButton(
                            onClick = { trainerViewModel.disconnectDevice(device.id) }
                        ) {
                            Text("Disconnect", color = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }

    // Device Selection Dialog
    if (showDeviceDialog) {
        // Timeout pentru dialog după 45 secunde
        LaunchedEffect(showDeviceDialog) {
            if (showDeviceDialog) {
                delay(45000)
                if (connectionState == ConnectionState.SCANNING || connectionState == ConnectionState.CONNECTING) {
                    Log.w("WorkoutExecution", "HR Dialog timeout - closing")
                    showDeviceDialog = false
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = { Text("Select Heart Rate Device") },
            text = {
                Column {
                    if (connectionState == ConnectionState.SCANNING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning for heart rate devices...")
                        }
                    } else if (connectionState == ConnectionState.CONNECTING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting to heart rate device...")
                        }
                    } else {
                        if (availableDevices.isEmpty()) {
                            Text(
                                text = "No heart rate devices found. Try scanning again.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        } else {
                            availableDevices.forEach { device ->
                                TextButton(
                                    onClick = {
                                        trainerViewModel.connectToDevice(device)
                                        showDeviceDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${device.name} (${device.type})")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun CurrentWorkoutStepCard(session: WorkoutSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Current Step: ${session.currentStep + 1}/${session.totalSteps}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Elapsed Time: ${formatTime(session.elapsedTime)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )

            // Progress bar
            LinearProgressIndicator(
                progress = if (session.totalSteps > 0) (session.currentStep + 1).toFloat() / session.totalSteps else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = Color(0xFF6366F1)
            )

            // Afișează detaliile pasului curent
            session.trainingPlan.steps?.getOrNull(session.currentStep)?.let { step ->
                when (step) {
                    is WorkoutStep.SteadyState -> {
                        Text("Steady State: ${step.power.toInt()}W for ${formatTime(step.duration)}")
                    }
                    is WorkoutStep.IntervalsT -> {
                        Text("Intervals: ${step.repeat}x (${step.on_power.toInt()}W/${step.off_power.toInt()}W)")
                    }
                    is WorkoutStep.Ramp -> {
                        Text(
                            "Ramp: ${step.start_power.toInt()}W → ${step.end_power.toInt()}W in ${
                                formatTime(
                                    step.duration
                                )
                            }"
                        )
                    }
                    is WorkoutStep.FreeRide -> {
                        Text(
                            "Free Ride: ${step.power_low.toInt()}W - ${step.power_high.toInt()}W for ${
                                formatTime(
                                    step.duration
                                )
                            }"
                        )
                    }
                    is WorkoutStep.IntervalsP -> {
                        Text("Power Intervals: ${step.repeat}x (${step.on_power.toInt()}W/${step.off_power.toInt()}W)")
                    }
                    is WorkoutStep.Pyramid -> {
                        Text("Pyramid: ${step.repeat}x (${step.start_power.toInt()}W → ${step.peak_power.toInt()}W → ${step.end_power.toInt()}W)")
                    }
                }
            }
        }
    }
}

@Composable
private fun RealTimeMetricsCard(data: RealTimeData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Real-time Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Putere", "${data.power}W", Color(0xFF6366F1))
                MetricItem("Puls", "${data.heartRate} bpm", Color(0xFFEF4444))
                MetricItem("Cadență", "${data.cadence} rpm", Color(0xFF10B981))
                MetricItem("Viteză", "${String.format("%.1f", data.speed)} km/h", Color(0xFFF59E0B))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                MetricItem(
                    "Distance",
                    "${String.format("%.2f", data.distance)} km",
                    Color(0xFF8B5CF6)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun WorkoutControlsCard(
    modifier: Modifier = Modifier,
    session: WorkoutSession,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (session.isPaused) {
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "Resume",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            } else {
                Button(
                    onClick = onPause,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = "Pause",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "Pause",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Button(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Skip",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    "Skip",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    "Stop",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
        else -> String.format("%d:%02d", minutes, secs)
    }
}

@Composable
private fun ErgStatusCard(
    ergMode: Boolean,
    targetPower: Int,
    actualPower: Int,
    trainerViewModel: TrainerViewModel // Adaugă parametrul
) {
    val currentFtp = trainerViewModel.getCurrentFtp()
    val powerZone = trainerViewModel.getCurrentPowerZoneString()
    val ftpPercentage = if (currentFtp > 0) ((targetPower.toFloat() / currentFtp) * 100).toInt() else 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ergMode) Color(0xFFECFDF5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ERG Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentFtp > 0) {
                        Text(
                            text = "FTP: ${currentFtp}W",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ergMode) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "ERG Active",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "ERG Inactive",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "INACTIVE",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (ergMode) {
                Spacer(modifier = Modifier.height(12.dp))

                // Power Zone Info
                Text(
                    text = powerZone,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6366F1),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Target Power
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Target",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = "${targetPower}W",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                        if (currentFtp > 0) {
                            Text(
                                text = "${ftpPercentage}% FTP",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color(0xFFE5E7EB))
                    )

                    // Actual Power
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Actual",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = "${actualPower}W",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (kotlin.math.abs(actualPower - targetPower) <= 10) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        if (currentFtp > 0) {
                            val actualFtpPercentage = ((actualPower.toFloat() / currentFtp) * 100).toInt()
                            Text(
                                text = "${actualFtpPercentage}% FTP",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color(0xFFE5E7EB))
                    )

                    // Difference
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Diff",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                        val diff = actualPower - targetPower
                        Text(
                            text = "${if (diff > 0) "+" else ""}${diff}W",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                kotlin.math.abs(diff) <= 10 -> Color(0xFF10B981)
                                diff > 0 -> Color(0xFFEF4444)
                                else -> Color(0xFFF59E0B)
                            }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ERG mode will automatically control trainer resistance based on workout targets",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}
