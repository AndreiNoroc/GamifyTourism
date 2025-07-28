package com.example.myfindgo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch

@Composable
fun ConnectDeviceScreen(
    onRetry: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text(
                    text = "Connect Your Device",
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Please connect your watch with an Android phone to continue",
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onRetry() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry Connection")
                }
            }
        }
    }
}