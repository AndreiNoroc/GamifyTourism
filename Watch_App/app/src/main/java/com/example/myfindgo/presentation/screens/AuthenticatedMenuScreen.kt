package com.example.myfindgo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class ScoreResponse(
    val score: Int
)

interface ScoreApi {
    @GET("/score")
    suspend fun getScore(@Query("username") username: String): ScoreResponse
}

@Composable
fun AuthenticatedMenuScreen(
    onMapClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onVisitedLocationsClick: () -> Unit,
    onChallengesClick: () -> Unit,
    onScoreClick: () -> Unit,
    username: String
) {
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()
    var score by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://gamifytourism-955657412481.europe-west9.run.app")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(ScoreApi::class.java)
                val response = withContext(Dispatchers.IO) {
                    api.getScore(username)
                }
                score = response.score
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
                    text = "Welcome, $username",
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onMapClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF4285F4) // Google Blue
                    )
                ) {
                    Text("MAP")
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onLeaderboardClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFEA4335) // Google Red
                    )
                ) {
                    Text("Leaderboard")
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onVisitedLocationsClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFFBBC05) // Google Yellow
                    )
                ) {
                    Text("Visited Locations")
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onChallengesClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF34A853) // Google Green
                    )
                ) {
                    Text("Daily Challenges")
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onScoreClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF4285F4) // Google Blue
                    )
                ) {
                    if (isLoading) {
                        Text("Loading Score...")
                    } else if (error != null) {
                        Text("Error Loading Score")
                    } else {
                        Text("Score: ${score ?: 0}")
                    }
                }
            }
        }
    }
} 