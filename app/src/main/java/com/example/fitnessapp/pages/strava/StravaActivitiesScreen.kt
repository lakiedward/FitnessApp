package com.example.fitnessapp.pages.strava

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.AuthState
import androidx.compose.ui.tooling.preview.Preview
import com.example.fitnessapp.mock.SharedPreferencesMock
import androidx.compose.ui.platform.LocalContext
import com.example.fitnessapp.ui.theme.FitnessAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StravaActivitiesScreen(
    stravaViewModel: StravaViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val activities by stravaViewModel.stravaActivities.collectAsState()
    val authState by authViewModel.authState.observeAsState()
    val jwtToken = (authState as? AuthState.Authenticated)?.jwtToken
    val ftpEstimate by stravaViewModel.ftpEstimate.collectAsState()

    // Removed fetchStravaActivities call - activities are managed by sync-live

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Strava Activities") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    // Removed fetchStravaActivities call - activities are managed by sync-live
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Activities from Sync")
                }
                Button(
                    onClick = { stravaViewModel.estimateFtp() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Estimează FTP")
                }
            }
            if (ftpEstimate != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("FTP estimat: ${ftpEstimate?.estimatedFTP?.toInt() ?: "-"} W", style = MaterialTheme.typography.titleMedium)
                        Text("Încredere: ${(ftpEstimate?.confidence?.times(100)?.toInt() ?: 0)}%", style = MaterialTheme.typography.bodySmall)
                        Text("Metodă: ${ftpEstimate?.method ?: "-"}", style = MaterialTheme.typography.bodySmall)
                        if (!ftpEstimate?.notes.isNullOrEmpty()) {
                            Text("Note: ${ftpEstimate?.notes}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No activities found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(activities) { activity ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = activity.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = "Tip: ${activity.type}")
                                Text(
                                    text = "Distanță: ${
                                        activity.distance?.let { "%.2f km".format(it / 1000) } ?: "N/A"
                                    }"
                                )
                                Text(
                                    text = "Durată: ${formatDuration(activity.movingTime)}"
                                )
                                Text(
                                    text = "Viteză medie: ${
                                        activity.averageSpeed?.let { "%.2f km/h".format(it * 3.6) } ?: "N/A"
                                    }"
                                )
                                Text(
                                    text = "Puls mediu: ${
                                        activity.averageHeartrate?.let { "%.0f bpm".format(it) } ?: "N/A"
                                    }"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) "%dh %02dm".format(hours, minutes)
    else "%dm %02ds".format(minutes, secs)
}

@Preview(showBackground = true)
@Composable
fun StravaActivitiesScreenPreview() {
    val context = LocalContext.current
    val authViewModel = AuthViewModel(SharedPreferencesMock())
    val stravaViewModel = com.example.fitnessapp.viewmodel.StravaViewModel.getInstance(context)
    FitnessAppTheme {
        StravaActivitiesScreen(
            stravaViewModel = stravaViewModel,
            authViewModel = authViewModel,
            onNavigateBack = {}
        )
    }
}
