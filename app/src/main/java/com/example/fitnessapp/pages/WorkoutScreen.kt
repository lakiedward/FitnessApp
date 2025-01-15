package com.example.fitnessapp.pages

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import com.example.fitnessapp.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions


@Composable
fun WorkoutScreen(navController: NavHostController) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val currentLocation = remember { mutableStateOf<LatLng?>(null) }
    val pathPoints = remember { mutableStateListOf<LatLng>() }
    val permissionGranted = remember { mutableStateOf(false) }
    var currentMarker by remember { mutableStateOf<Marker?>(null) }
    var totalDistance by remember { mutableStateOf(0f) } // Variabilă pentru distanța totală

    // Request location permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted.value = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.back),
                    contentDescription = "Back"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                LaunchedEffect(Unit) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        permissionGranted.value = true
                    }
                }

                if (!permissionGranted.value) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Location permission is required to track your workout.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }) {
                            Text("Grant Permission")
                        }
                    }
                } else {
                    AndroidView(factory = { context ->
                        MapView(context).apply {
                            onCreate(null)
                            onResume()
                            MapsInitializer.initialize(context)
                        }
                    }) { mapView ->
                        mapView.getMapAsync { googleMap ->
                            val locationRequest = LocationRequest.create().apply {
                                interval = 5000 // 5 seconds
                                fastestInterval = 2000
                                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                            }

                            val locationCallback = object : LocationCallback() {
                                override fun onLocationResult(locationResult: LocationResult) {
                                    super.onLocationResult(locationResult)
                                    val newLocation = locationResult.lastLocation
                                    if (newLocation != null) {
                                        val latLng =
                                            LatLng(newLocation.latitude, newLocation.longitude)
                                        if (currentLocation.value != latLng) {
                                            if (pathPoints.isNotEmpty()) {
                                                val lastPoint = pathPoints.last()
                                                val result = FloatArray(1)
                                                Location.distanceBetween(
                                                    lastPoint.latitude, lastPoint.longitude,
                                                    latLng.latitude, latLng.longitude,
                                                    result
                                                )
                                                totalDistance += result[0] // Actualizează distanța totală
                                            }
                                            pathPoints.add(latLng)
                                            currentLocation.value = latLng
                                        }

                                        googleMap.clear()

                                        // Actualizează doar marker-ul curent
                                        currentMarker?.remove() // Șterge marker-ul anterior, dacă există
                                        currentMarker = googleMap.addMarker(
                                            MarkerOptions()
                                                .position(latLng)
                                                .title("Current Location")
                                        )

                                        // Adaugă linia traseului (dacă există cel puțin două puncte)
                                        if (pathPoints.size > 1) {
                                            googleMap.addPolyline(
                                                PolylineOptions()
                                                    .addAll(pathPoints)
                                                    .width(5f)
                                                    .color(Color.Blue.hashCode())
                                            )
                                        }

                                        googleMap.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                        )
                                    }
                                }
                            }

                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                fusedLocationClient.requestLocationUpdates(
                                    locationRequest,
                                    locationCallback,
                                    Looper.getMainLooper()
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "Total Distance: ${"%.2f".format(totalDistance / 1000)} km", // Afișează distanța în km
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            var isWorkoutStarted by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    isWorkoutStarted = !isWorkoutStarted
                    if (!isWorkoutStarted) {
                        navController.navigate("home_screen") // Navighează la HomeScreen
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = if (isWorkoutStarted) "Finish Workout" else "Start Workout")
            }
        }
    }
}
