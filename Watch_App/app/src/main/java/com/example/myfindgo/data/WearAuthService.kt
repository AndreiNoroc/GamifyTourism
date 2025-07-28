package com.example.myfindgo.data

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.*
import android.content.Context
import kotlinx.coroutines.tasks.await

class WearAuthService : WearableListenerService() {
    companion object {
        private const val AUTH_TOKEN_PREF = "auth_token"
        private const val PREFS_NAME = "WearAuthPrefs"
        private const val TAG = "WearAuthService"
        private const val AUTH_PATH = "/auth"

        fun getStoredAuthToken(context: Context): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(AUTH_TOKEN_PREF, null)
        }

        suspend fun authenticate(context: Context, email: String, password: String) {
            Log.d(TAG, "Starting authentication process")
            val nodeClient = Wearable.getNodeClient(context)
            val messageClient = Wearable.getMessageClient(context)

            try {
                // Get connected nodes
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.e(TAG, "No connected nodes found")
                    throw Exception("No connected nodes found")
                }

                // Prepare auth data
                val authData = "$email:$password"

                // Send auth request to all connected nodes
                nodes.forEach { node ->
                    Log.d(TAG, "Sending auth request to node: ${node.displayName}")
                    val result = messageClient.sendMessage(
                        node.id,
                        AUTH_PATH,
                        authData.toByteArray()
                    ).await()

                    Log.d(TAG, "Auth request sent to ${node.displayName}, result: $result")
                }

                // Wait for auth response
                val dataClient = Wearable.getDataClient(context)
                val dataItems = dataClient.dataItems.await()

                var authSuccessful = false
                dataItems.forEach { dataItem ->
                    if (dataItem.uri.path == AUTH_PATH) {
                        val response = String(dataItem.data!!)
                        Log.d(TAG, "Received auth response: $response")
                        if (response == "success") {
                            authSuccessful = true
                        }
                    }
                }

                if (!authSuccessful) {
                    Log.e(TAG, "Authentication failed")
                    throw Exception("Authentication failed")
                }

                Log.d(TAG, "Authentication successful")
            } catch (e: Exception) {
                Log.e(TAG, "Error during authentication: ${e.message}")
                throw e
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            "/auth" -> {
                val authToken = String(messageEvent.data)
                handleAuthToken(authToken)
            }
            "/request_auth" -> {
                handleAuthRequest(messageEvent.sourceNodeId)

            }
            "/logout" -> {
                handleLogout()
            }
        }
    }

    private fun handleAuthToken(authToken: String) {
        try {
            // Store the auth token
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(AUTH_TOKEN_PREF, authToken)
                .apply()

            Log.d("WearAuthService", "Successfully stored auth token")
            // Broadcast that authentication is complete
            sendBroadcast(Intent("com.example.myfindgo.AUTH_COMPLETE"))
        } catch (e: Exception) {
            Log.e("WearAuthService", "Failed to handle auth token", e)
        }
    }

    private fun handleAuthRequest(sourceNodeId: String) {
        try {
            // The mobile app will handle checking if the user is logged in
            // and send the auth token if available
            // We just need to send the request
            Wearable.getMessageClient(this).sendMessage(
                sourceNodeId,
                "/request_auth",
                byteArrayOf()
            ).addOnSuccessListener {
                Log.d("WearAuthService", "Auth request sent to mobile app")
            }.addOnFailureListener { e ->
                Log.e("WearAuthService", "Failed to send auth request", e)
            }
        } catch (e: Exception) {
            Log.e("WearAuthService", "Error handling auth request", e)
        }
    }

    private fun handleLogout() {
        // Clear the stored auth token
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(AUTH_TOKEN_PREF)
            .apply()

        sendBroadcast(Intent("com.example.myfindgo.LOGOUT_COMPLETE"))
    }
}