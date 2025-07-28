package com.example.myfindgo.presentation.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import com.example.myfindgo.data.api.VisitedLocationsApi
import com.example.myfindgo.data.models.Location
import com.example.myfindgo.data.repositories.LocationRepository
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import com.google.maps.model.LatLng as MapsLatLng
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Define route color as a constant
private val ROUTE_COLOR = Color(0xFF0000FF) // Blue color in ARGB format
private val VISITED_COLOR = Color(0xFF34A853) // Google Green for visited locations
private val UNVISITED_COLOR = Color(0xFF4285F4) // Google Blue for unvisited locations

data class RoutePoints(
    val points: List<LatLng>
)

private const val TAG = "MapScreen"

@Composable
fun MapScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locations by remember { mutableStateOf<List<Location>>(emptyList()) }
    var visitedLocations by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var showTransportDialog by remember { mutableStateOf(false) }
    var routePoints by remember { mutableStateOf<RoutePoints?>(null) }
    val locationRepository = remember { LocationRepository() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = LatLng(location.latitude, location.longitude)
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            scope.launch {
                startLocationUpdates(fusedLocationClient, locationCallback)
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            Log.d(TAG, "Starting to fetch locations...")
            try {
                // Fetch locations
                locations = locationRepository.getLocations()
                Log.d(TAG, "Successfully fetched ${locations.size} locations")
                locations.forEach { location ->
                    Log.d(TAG, "Location: ${location.name}, Score: ${location.score}, Lat: ${location.latitude}, Lng: ${location.longitude}")
                }

                // Fetch visited locations
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://gamifytourism-955657412481.europe-west9.run.app")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(VisitedLocationsApi::class.java)
                val response = withContext(Dispatchers.IO) {
                    api.getVisitedLocations(mapOf("username" to "andrei"))
                }
                visitedLocations = response.visitedLocations
                Log.d(TAG, "Successfully fetched ${visitedLocations.size} visited locations")

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data: ${e.message}")
            }

            if (hasLocationPermission(context)) {
                Log.d(TAG, "Location permission granted, starting location updates")
                startLocationUpdates(fusedLocationClient, locationCallback)
            } else {
                Log.d(TAG, "Requesting location permissions")
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = currentLocation?.let { CameraPosition.fromLatLngZoom(it, 15f) }
            ?: CameraPosition.fromLatLngZoom(LatLng(41.0082, 28.9784), 12f)
    }

    // Only center on current location when the app starts
    LaunchedEffect(Unit) {
        currentLocation?.let { location ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
        }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = {
            PositionIndicator(
                scalingLazyListState = rememberScalingLazyListState()
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Map content
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false
                )
            ) {
                // Location markers
                locations.forEach { location ->
                    val isVisited = visitedLocations.contains(location.name)
                    Marker(
                        state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                        title = location.name,
                        snippet = "Score: ${location.score}",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (isVisited) {
                                BitmapDescriptorFactory.HUE_GREEN
                            } else {
                                BitmapDescriptorFactory.HUE_ORANGE
                            }
                        ),
                        onClick = {
                            Log.d(TAG, "Marker clicked: ${location.name}")
                            selectedLocation = location
                            showTransportDialog = true
                            true
                        }
                    )
                }

                // Route polyline
                routePoints?.let { points ->
                    Polyline(
                        points = points.points,
                        color = ROUTE_COLOR,
                        width = 5f
                    )
                }
            }

            // Back button
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 32.dp, start = 8.dp)
                    .size(48.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF4285F4) // Google Blue
                )
            ) {
                Text("←", style = MaterialTheme.typography.title2)
            }

            // Show transport mode dialog
            if (showTransportDialog && selectedLocation != null) {
                Log.d(TAG, "Showing transport dialog for location: ${selectedLocation?.name}")
                Alert(
                    title = { Text("Choose Transport Mode") },
                    content = {
                        item {
                             Button(
                                onClick = {
                                    Log.d(TAG, "Walking button clicked")
                                    scope.launch {
                                        val points = calculateWalkingRoute(currentLocation, selectedLocation!!)
                                        routePoints = points
                                        showTransportDialog = false
                                    }
                                }
                            ) {
                                Text("Start Walking")
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        item {
                            Button(
                                onClick = {
                                    Log.d(TAG, "Driving button clicked")
                                    // You can implement driving here later
                                    showTransportDialog = false
                                }
                            ) {
                                Text("Start Driving")
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

private suspend fun startLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    locationCallback: LocationCallback
) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
        .setMinUpdateIntervalMillis(500)
        .build()

    try {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        ).await()
    } catch (e: Exception) {
        // Handle error
    }
}

private suspend fun calculateWalkingRoute(
    startLocation: LatLng?,
    destination: Location
): RoutePoints? {
    if (startLocation == null) {
        Log.d(TAG, "Cannot calculate route: start location is null")
        return null
    }

    Log.d(TAG, "Calculating walking route from: (${startLocation.latitude}, ${startLocation.longitude}) to: (${destination.latitude}, ${destination.longitude})")

    val context = GeoApiContext.Builder()
        .apiKey("AIzaSyCyH23v5bzgiL3JL9xf22w4GoEvwqx45vQ")
        .build()

    return try {
        val result = DirectionsApi.newRequest(context)
            .origin(MapsLatLng(startLocation.latitude, startLocation.longitude))
            .destination(MapsLatLng(destination.latitude, destination.longitude))
            .mode(TravelMode.WALKING)
            .await()

        if (result.routes.isNotEmpty()) {
            val route = result.routes[0]
            Log.d(TAG, "Route calculated successfully. Distance: ${route.legs[0].distance}, Duration: ${route.legs[0].duration}")
            val points = route.overviewPolyline.decodePath()
                .map { LatLng(it.lat, it.lng) }

            RoutePoints(points)
        } else {
            Log.d(TAG, "No routes found")
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error calculating route: ${e.message}")
        null
    }
}