package com.example.fitnessapp.pages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.fitnessapp.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class WorkoutViewModel(context: Context) : ViewModel() {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var googleMap: GoogleMap? = null
    private var directionArrow: Marker? = null
    private var previousLocation: Location? = null
    private val appContext = context.applicationContext

    // New state variables
    var activityType = mutableStateOf("Running")
    var caloriesBurned = mutableStateOf(0f)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location -> // Renamed parameter
                val newLatLng = LatLng(location.latitude, location.longitude)
                val bearing = calculateBearing(location)

                // Explicitly reference the ViewModel's property
                this@WorkoutViewModel.currentLocation.value = newLatLng
                pathPoints.add(newLatLng)

                previousLocation?.let { prev ->
                    val distanceDelta = prev.distanceTo(location)
                    totalDistance.value += distanceDelta
                    caloriesBurned.value += calculateCaloriesBurned(distanceDelta, activityType.value)
                }

                updateMap(newLatLng, bearing)
                previousLocation = location
            }
        }
    }

    var pathPoints = mutableStateListOf<LatLng>()
    var totalDistance = mutableStateOf(0f)
    var currentLocation = mutableStateOf<LatLng?>(null)
    var isWorkoutActive = mutableStateOf(false)
    var workoutDuration = mutableStateOf(0L)
    private var currentPolyline: Polyline? = null

    private fun calculateBearing(currentLocation: Location): Float {
        return previousLocation?.bearingTo(currentLocation) ?: 0f
    }

    private fun calculateCaloriesBurned(distanceMeters: Float, activity: String): Float {
        val distanceKm = distanceMeters / 1000
        return when (activity) {
            "Running" -> distanceKm * 60  // 60 calories per km
            "Cycling" -> distanceKm * 30  // 30 calories per km
            else -> 0f
        }
    }

    fun initializeMap(map: GoogleMap) {
        googleMap = map.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
        }
    }

    private fun scaleBitmap(
        resourceId: Int,
        widthDp: Int,
        heightDp: Int
    ): Bitmap {
        val bitmap = BitmapFactory.decodeResource(appContext.resources, resourceId)
        val density = appContext.resources.displayMetrics.density
        val scaledWidth = (widthDp * density).toInt()
        val scaledHeight = (heightDp * density).toInt()
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)
    }

    private fun updateMap(position: LatLng, bearing: Float) {
        googleMap?.let { map ->
            directionArrow?.let {
                it.position = position
                it.rotation = bearing
            } ?: run {
                directionArrow = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .icon(BitmapDescriptorFactory.fromBitmap(
                            scaleBitmap(
                                R.drawable.ic_navigation,
                                40,
                                40
                            )
                        ))
                        .anchor(0.5f, 0.5f)
                        .rotation(bearing)
                )
            }

            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 17f))

            if (pathPoints.size > 1) {
                currentPolyline?.remove()
                currentPolyline = map.addPolyline(
                    PolylineOptions()
                        .addAll(pathPoints)
                        .width(8f)
                        .color(Color.Blue.hashCode())
                )
            }
        }
    }

    fun startLocationUpdates(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.create().apply {
                interval = 5000
                fastestInterval = 2000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun resetWorkout() {
        pathPoints.clear()
        totalDistance.value = 0f
        caloriesBurned.value = 0f
        workoutDuration.value = 0L
        previousLocation = null
        currentPolyline?.remove()
        currentPolyline = null
        directionArrow?.remove()
        directionArrow = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModelFactory(context))
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && viewModel.isWorkoutActive.value) {
            viewModel.startLocationUpdates(context)
        }
    }

    LaunchedEffect(viewModel.isWorkoutActive.value) {
        if (viewModel.isWorkoutActive.value) {
            while (true) {
                delay(1000)
                viewModel.workoutDuration.value++
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            mapView?.let {
                when (event) {
                    Lifecycle.Event.ON_START -> it.onStart()
                    Lifecycle.Event.ON_RESUME -> it.onResume()
                    Lifecycle.Event.ON_PAUSE -> it.onPause()
                    Lifecycle.Event.ON_STOP -> it.onStop()
                    Lifecycle.Event.ON_DESTROY -> {
                        it.onDestroy()
                        viewModel.resetWorkout()
                    }
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Tracking") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                AndroidView(
                    factory = { context ->
                        MapView(context).apply {
                            mapView = this
                            onCreate(null)
                            getMapAsync { googleMap ->
                                viewModel.initializeMap(googleMap)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                WorkoutStats(viewModel)
                ControlButtons(viewModel, context, permissionLauncher, navController)
            } else {
                PermissionRequestView(permissionLauncher)
            }
        }
    }
}

@Composable
private fun WorkoutStats(viewModel: WorkoutViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                label = "Calories",
                value = "%.0f kcal".format(viewModel.caloriesBurned.value)
            )
            ActivityTypeDropdown(viewModel)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(
                label = "Distance",
                value = "%.2f km".format(viewModel.totalDistance.value / 1000)
            )
            StatItem(
                label = "Time",
                value = formatDuration(viewModel.workoutDuration.value)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityTypeDropdown(viewModel: WorkoutViewModel) {
    val options = listOf("Running", "Cycling")
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.width(150.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                modifier = Modifier.menuAnchor(),
                readOnly = true,
                value = viewModel.activityType.value,
                onValueChange = {},
                label = { Text("Activity") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.activityType.value = option
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ControlButtons(
    viewModel: WorkoutViewModel,
    context: Context,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    navController: NavHostController
) {
    Button(
        onClick = {
            if (!viewModel.isWorkoutActive.value) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.isWorkoutActive.value = true
                    viewModel.startLocationUpdates(context)
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                viewModel.isWorkoutActive.value = false
                viewModel.stopLocationUpdates()
                navController.navigate("home_screen")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(if (viewModel.isWorkoutActive.value) "Finish Workout" else "Start Workout")
    }
}

@Composable
private fun PermissionRequestView(
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Location permission is required to track your workout.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
            Text("Grant Permission")
        }
    }
}

private fun formatDuration(seconds: Long): String {
    return String.format(
        "%02d:%02d:%02d",
        TimeUnit.SECONDS.toHours(seconds),
        TimeUnit.SECONDS.toMinutes(seconds) % 60,
        seconds % 60
    )
}

class WorkoutViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}