package com.example.myfindgo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.myfindgo.data.api.VisitedLocationsApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.compose.ui.graphics.Color

// Google brand colors
private val googleBlue = Color(0xFF4285F4)

@Composable
fun VisitedLocationsScreen(
    onBackClick: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()
    var visitedLocations by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://gamifytourism-955657412481.europe-west9.run.app")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(VisitedLocationsApi::class.java)
                val response = withContext(Dispatchers.IO) {
                    api.getVisitedLocations(mapOf("username" to "andrei"))
                }
                visitedLocations = response.visitedLocations
            } catch (e: Exception) {
                error = e.message ?: "An error occurred"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Visited Locations",
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (error != null) {
                item {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                visitedLocations.forEach { location ->
                    item {
                        Text(
                            text = location,
                            style = MaterialTheme.typography.body1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp),
                            color = androidx.compose.ui.graphics.Color.Black
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = googleBlue
                    )
                ) {
                    Text("Back")
                }
            }
        }
    }
} 