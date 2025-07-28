package com.example.myfindgo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*

// Google brand colors
private val googleBlue = Color(0xFF4285F4)
private val googleRed = Color(0xFFEA4335)
private val googleYellow = Color(0xFFFBBC05)
private val googleGreen = Color(0xFF34A853)

@Composable
fun MenuScreen(
    onMapClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onVisitedLocationsClick: () -> Unit,
    onChallengesClick: () -> Unit,
    isGuestMode: Boolean
) {
    val listState = rememberScalingLazyListState()

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
                    text = if (isGuestMode) "Guest Mode" else "Menu",
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
                        backgroundColor = googleBlue
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
                        backgroundColor = googleRed
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
                        backgroundColor = googleYellow
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
                        backgroundColor = googleGreen
                    )
                ) {
                    Text("Daily Challenges")
                }
            }
        }
    }
}