package com.example.fitnessapp.pages.more

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessapp.R
import com.example.fitnessapp.viewmodel.StravaViewModel
import kotlinx.coroutines.launch
import com.example.fitnessapp.viewmodel.StravaState
import androidx.compose.runtime.livedata.observeAsState
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.AuthState
import androidx.navigation.NavController

private const val TAG = "AppIntegrationsScreen"

@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.iclogo),
            contentDescription = "App Logo",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Connect",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIntegrationsScreen(
    onNavigateToStravaActivities: () -> Unit,
    onNavigateToActivitiesBySport: () -> Unit = {},
    authViewModel: AuthViewModel,
    stravaViewModel: StravaViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stravaState by stravaViewModel.stravaState.collectAsState()
    val stravaUserData by stravaViewModel.stravaUserData.collectAsState()
    val stravaAthlete by stravaViewModel.stravaAthlete.collectAsState()
    val authState = authViewModel.authState.observeAsState()

    val handleConnect: () -> Unit = {
        scope.launch {
            try {
                val authUrl = stravaViewModel.getAuthUrl()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting Strava auth flow", e)
                Toast.makeText(context, "Error connecting to Strava: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            TopBar()
            
            Spacer(modifier = Modifier.height(32.dp))

            // Banner de autentificare
            if (authState.value is AuthState.Authenticated) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color(0xFF4CAF50), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ești autentificat!",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "App Integrations",
                    style = MaterialTheme.typography.headlineMedium
                )

                // Strava Integration Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_strava),
                                    contentDescription = "Strava Logo",
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Strava Integration",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            
                            // Status icon
                            when (stravaState) {
                                is StravaState.Connected -> {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check_circle),
                                        contentDescription = "Connected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                is StravaState.Connecting -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                else -> {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_link),
                                        contentDescription = "Not Connected",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        when (stravaState) {
                            is StravaState.Connected -> {
                                // Connected state
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                            Text(
                                            text = "Connected as ${stravaAthlete?.firstName ?: "User"}",
                                style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        // Buton pentru activități Strava (vizibil doar dacă userul e autentificat)
                                        if (authState.value is AuthState.Authenticated) {
                                            Button(
                                                onClick = onNavigateToStravaActivities,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.Gray
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Vezi activitățile din Strava")
                                            }
                                        }
                                        TextButton(
                                            onClick = { stravaViewModel.disconnect() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Disconnect")
                            }
                        }
                    }
                }
                            is StravaState.Connecting -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        "Connecting to Strava...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            is StravaState.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = (stravaState as StravaState.Error).message,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Button(
                                            onClick = handleConnect,
                                            modifier = Modifier.align(Alignment.End),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            )
                                        ) {
                                            Text("Try Again")
                                        }
                                    }
                                }
                            }
                            else -> {
                                Button(
                                    onClick = handleConnect,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Gray
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_strava),
                                            contentDescription = "Strava Logo",
                                            modifier = Modifier.size(24.dp)
                    )
                                        Text("Connect to Strava")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IntegrationStatusCard(
    logoResId: Int,
    appName: String,
    description: String,
    isConnected: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit = {}
) {
    val borderColor = if (isConnected) Color(0xFF4CAF50) else Color.LightGray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = "$appName logo",
                    modifier = Modifier
                        .height(36.dp)
                        .padding(end = 12.dp)
                )

                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.DarkGray)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isConnected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConnectedStatus(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connected", color = Color(0xFF4CAF50))
                    }

                    TextButton(onClick = onDisconnectClick) {
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_link),
                            contentDescription = "Connect",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect")
                    }

                }
            }
        }
    }
}

@Composable
fun ConnectedStatus(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = R.drawable.ic_check_circle),
        contentDescription = "Connected",
        tint = Color(0xFF4CAF50),
        modifier = modifier
    )
}


