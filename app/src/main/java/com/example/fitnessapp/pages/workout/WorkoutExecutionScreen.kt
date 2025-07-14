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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExecutionScreen(
    trainingId: Int,
    training: TrainingPlan,
    navController: NavController
) {
    val context = LocalContext.current
    val trainerViewModel: TrainerViewModel = viewModel(
        factory = TrainerViewModelFactory(context)
    )
    val currentSession by trainerViewModel.currentSession.observeAsState()
    val realTimeData by trainerViewModel.realTimeData.observeAsState(RealTimeData())
    val connectionState by trainerViewModel.connectionState.observeAsState(ConnectionState.DISCONNECTED)
    val connectedDevices by trainerViewModel.connectedDevices.observeAsState(emptyList())
    val availableDevices by trainerViewModel.availableDevices.observeAsState(emptyList())

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top App Bar
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header cu informații despre antrenament
            WorkoutHeader(training)

            // Status conexiune trainer
            TrainerConnectionCard(connectionState, connectedDevices, availableDevices, trainerViewModel, ::requestBluetoothPermissions)

            // Pasul curent din antrenament
            if (currentSession != null) {
                CurrentWorkoutStepCard(currentSession!!)

                // Date în timp real
                RealTimeMetricsCard(realTimeData)

                // Controale antrenament
                WorkoutControlsCard(
                    session = currentSession!!,
                    onStart = { trainerViewModel.startWorkout(training) },
                    onPause = { trainerViewModel.pauseWorkout() },
                    onResume = { trainerViewModel.resumeWorkout() },
                    onStop = { 
                        trainerViewModel.stopWorkout()
                        navController.navigateUp()
                    },
                    onSkip = { trainerViewModel.skipToNextStep() }
                )
            } else {
                // Buton pentru a începe antrenamentul
                Button(
                    onClick = { trainerViewModel.startWorkout(training) },
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
                        text = "Începe antrenamentul",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
                text = "Durată: ${training.duration} | Intensitate: ${training.intensity}",
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
private fun CurrentWorkoutStepCard(session: WorkoutSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Pasul curent: ${session.currentStep + 1}/${session.totalSteps}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Timp scurs: ${formatTime(session.elapsedTime)}",
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
                        Text("Steady State: ${step.power.toInt()}W pentru ${formatTime(step.duration)}")
                    }
                    is WorkoutStep.IntervalsT -> {
                        Text("Intervale: ${step.repeat}x (${step.on_power.toInt()}W/${step.off_power.toInt()}W)")
                    }
                    is WorkoutStep.Ramp -> {
                        Text("Ramp: ${step.start_power.toInt()}W → ${step.end_power.toInt()}W în ${formatTime(step.duration)}")
                    }
                    is WorkoutStep.FreeRide -> {
                        Text("Free Ride: ${step.power_low.toInt()}W - ${step.power_high.toInt()}W pentru ${formatTime(step.duration)}")
                    }
                    is WorkoutStep.IntervalsP -> {
                        Text("Intervale Putere: ${step.repeat}x (${step.on_power.toInt()}W/${step.off_power.toInt()}W)")
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
                text = "Date în timp real",
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
                MetricItem("Distanță", "${String.format("%.2f", data.distance)} km", Color(0xFF8B5CF6))
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
    session: WorkoutSession,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (session.isPaused) {
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume")
                }
            } else {
                Button(
                    onClick = onPause,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pause")
                }
            }

            Button(
                onClick = onSkip,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Skip")
            }

            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stop")
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
