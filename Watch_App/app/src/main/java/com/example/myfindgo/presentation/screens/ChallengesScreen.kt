package com.example.myfindgo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import androidx.compose.ui.graphics.Color

data class Challenge(
    val title: String,
    val points: Int
)

interface ChallengesApi {
    @GET("/challenges")
    suspend fun getChallenges(): List<Challenge>
}

// Google brand colors
private val googleBlue = Color(0xFF4285F4)

@Composable
fun ChallengesScreen(
    onBackClick: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()
    var challenges by remember { mutableStateOf<List<Challenge>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://gamifytourism-955657412481.europe-west9.run.app")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(ChallengesApi::class.java)
                val response = withContext(Dispatchers.IO) {
                    api.getChallenges()
                }
                challenges = response
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
                    text = "Daily Challenges",
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
                challenges.forEach { challenge ->
                    item {
                        Card(
                            onClick = { /* Handle click if needed */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = challenge.title,
                                    style = MaterialTheme.typography.body1,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${challenge.points} points",
                                    style = MaterialTheme.typography.caption2,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
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