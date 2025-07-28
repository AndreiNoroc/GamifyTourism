package com.example.myfindgo.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import androidx.compose.material3.TextField
import com.example.myfindgo.data.WearAuthService
import com.example.myfindgo.data.WearSyncService
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "InitialScreen"

@Composable
fun InitialScreen(
    isDeviceSynced: Boolean,
    onEnterAsGuest: () -> Unit,
    onSyncDevice: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSyncDialog by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("") }

    val listState = rememberScalingLazyListState()

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
                    text = if (isDeviceSynced) "Device Synced" else "Device Not Synced",
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    color = if (isDeviceSynced) MaterialTheme.colors.primary else MaterialTheme.colors.error
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        Log.d(TAG, "Sync Device button clicked")
                        scope.launch {
                            try {
                                Log.d(TAG, "Starting sync process")
                                WearSyncService.syncData(context)
                                Log.d(TAG, "Sync completed successfully")
                                onSyncDevice()
                            } catch (e: Exception) {
                                Log.e(TAG, "Sync failed: ${e.message}")
                                syncStatus = "Sync failed: ${e.message}"
                                showSyncDialog = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sync Device")
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "",
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onEnterAsGuest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enter as Guest")
                }
            }
        }
    }

    if (showSyncDialog) {
        Log.d(TAG, "Showing sync dialog")
        Alert(
            title = { Text("Sync Status") },
            content = {
                Text(syncStatus)
            },
            negativeButton = {
                Button(onClick = {
                    Log.d(TAG, "Sync dialog cancelled")
                    showSyncDialog = false
                }) {
                    Text("OK")
                }
            },
            positiveButton = {
                Button(onClick = {
                    Log.d(TAG, "Sync dialog confirmed")
                    showSyncDialog = false
                }) {
                    Text("Close")
                }
            }
        )
    }
}