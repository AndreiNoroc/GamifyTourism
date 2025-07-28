/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.myfindgo.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material.*
import com.example.myfindgo.data.UserRepository
import com.example.myfindgo.data.WearAuthService
import com.example.myfindgo.data.WearSyncService
import com.example.myfindgo.presentation.screens.*
import com.example.myfindgo.presentation.theme.MyFindGoTheme
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private val userRepository = UserRepository()
    private var shouldNavigateToMenu by mutableStateOf(false)
    private var isDeviceSynced by mutableStateOf(false)
    private var isGuestMode by mutableStateOf(false)
    private var isCheckingConnection by mutableStateOf(false)
    private val messageClient by lazy { Wearable.getMessageClient(this) }

    private val authBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.myfindgo.AUTH_COMPLETE") {
                shouldNavigateToMenu = true
            }
        }
    }

    private suspend fun checkDeviceConnection() {
        if (isCheckingConnection) return

        isCheckingConnection = true
        try {
            val nodes = Wearable.getNodeClient(this).connectedNodes.await()
            isDeviceSynced = nodes.isNotEmpty()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking device connection", e)
            isDeviceSynced = false
        } finally {
            isCheckingConnection = false
        }
    }

    private suspend fun checkExistingAuth() {
        try {
            // Check if we already have a stored auth token
            val existingToken = WearAuthService.getStoredAuthToken(this)
            if (existingToken != null) {
                shouldNavigateToMenu = true
                return
            }

            // If no stored token, request one from the mobile app
            val nodes = Wearable.getNodeClient(this).connectedNodes.await()
            if (nodes.isNotEmpty()) {
                // Send request for auth token to all connected nodes
                nodes.forEach { node ->
                    try {
                        messageClient.sendMessage(
                            node.id,
                            "/request_auth",
                            byteArrayOf()
                        ).await()
                        Log.d("MainActivity", "Auth request sent to ${node.displayName}")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to send auth request", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking existing auth", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the broadcast receiver with RECEIVER_NOT_EXPORTED flag
        registerReceiver(
            authBroadcastReceiver,
            IntentFilter("com.example.myfindgo.AUTH_COMPLETE"),
            Context.RECEIVER_NOT_EXPORTED
        )

        setContent {
            MyFindGoTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                // Handle navigation when auth is complete
                LaunchedEffect(shouldNavigateToMenu) {
                    if (shouldNavigateToMenu) {
                        navController.navigate("menu") {
                            popUpTo("initial") { inclusive = true }
                        }
                        shouldNavigateToMenu = false
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "initial"
                ) {
                    composable("initial") {
                        InitialScreen(
                            isDeviceSynced = isDeviceSynced,
                            onEnterAsGuest = {
                                isGuestMode = true
                                navController.navigate("menu") {
                                    popUpTo("initial") { inclusive = true }
                                }
                            },
                            onSyncDevice = {
                                scope.launch {
                                    try {
                                        checkDeviceConnection()
                                        if (isDeviceSynced) {
                                            val username = WearSyncService.syncData(this@MainActivity)
                                            if (username != null) {
                                                navController.navigate("authenticated_menu/$username") {
                                                    popUpTo("initial") { inclusive = true }
                                                }
                                            } else {
                                                checkExistingAuth()
                                            }
                                        } else {
                                            navController.navigate("connect") {
                                                popUpTo("initial") { inclusive = true }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error in sync device", e)
                                        isDeviceSynced = false
                                    }
                                }
                            }
                        )
                    }
                    composable("connect") {
                        ConnectDeviceScreen(
                            onRetry = {
                                scope.launch {
                                    try {
                                        checkDeviceConnection()
                                        if (isDeviceSynced) {
                                            val username = WearSyncService.syncData(this@MainActivity)
                                            if (username != null) {
                                                navController.navigate("authenticated_menu/$username") {
                                                    popUpTo("initial") { inclusive = true }
                                                }
                                            } else {
                                                checkExistingAuth()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error in retry connection", e)
                                        isDeviceSynced = false
                                    }
                                }
                            }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("menu") {
                                    popUpTo("initial") { inclusive = true }
                                }
                            },
                            userRepository = userRepository
                        )
                    }
                    composable("menu") {
                        MenuScreen(
                            onMapClick = { navController.navigate("map") },
                            onLeaderboardClick = { navController.navigate("leaderboard") },
                            onVisitedLocationsClick = { navController.navigate("visited_locations") },
                            onChallengesClick = { navController.navigate("challenges") },
                            isGuestMode = isGuestMode
                        )
                    }
                    composable("authenticated_menu/{username}") { backStackEntry ->
                        val username = backStackEntry.arguments?.getString("username") ?: ""
                        AuthenticatedMenuScreen(
                            onMapClick = { navController.navigate("map") },
                            onLeaderboardClick = { navController.navigate("leaderboard") },
                            onVisitedLocationsClick = { navController.navigate("visited_locations") },
                            onChallengesClick = { navController.navigate("challenges") },
                            onScoreClick = { navController.navigate("score") },
                            username = username
                        )
                    }
                    composable("map") {
                        MapScreen(
                            onBackClick = { 
                                scope.launch {
                                     if (isDeviceSynced) {
                                        val username = WearSyncService.syncData(this@MainActivity)
                                        if (username != null) {
                                            navController.navigate("authenticated_menu/$username") {
                                                popUpTo("map") { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("menu") {
                                                popUpTo("map") { inclusive = true }
                                            }
                                        }
                                    } else {
                                        navController.navigate("menu") {
                                            popUpTo("map") { inclusive = true }
                                        }
                                    }
                                }
                            }
                        )
                    }
                    composable("score") {
                        ScoreScreen(userRepository = userRepository)
                    }
                    composable("leaderboard") {
                        LeaderboardScreen(
                            onBackClick = { navController.navigate("menu") }
                        )
                    }
                    composable("visited_locations") {
                        VisitedLocationsScreen(
                            onBackClick = { navController.navigate("menu") }
                        )
                    }
                    composable("challenges") {
                        ChallengesScreen(
                            onBackClick = { navController.navigate("menu") }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(authBroadcastReceiver)
    }
}