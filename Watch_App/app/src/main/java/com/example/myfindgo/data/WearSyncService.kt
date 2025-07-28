package com.example.myfindgo.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import kotlinx.coroutines.tasks.await

private const val TAG = "WearSyncService"
private const val SYNC_PATH = "/sync"
private const val SYNC_DATA_PATH = "/sync/data"
private const val AUTH_CHECK_PATH = "/auth/check"
private const val USERNAME_KEY = "username"

class WearSyncService {
    companion object {
        suspend fun syncData(context: Context): String? {
            Log.d(TAG, "Starting sync process")
            val nodeClient = Wearable.getNodeClient(context)
            val messageClient = Wearable.getMessageClient(context)

            val username = "andrei"

            try {
                // Get connected nodes
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.e(TAG, "No connected nodes found")
                    throw Exception("No connected nodes found")
                }

                Log.d(TAG, "Found ${nodes.size} connected nodes")
                nodes.forEach { node ->
                    Log.d(TAG, "Connected node: ${node.displayName} (${node.id})")
                }

                // Check authentication status
                nodes.forEach { node ->
                    Log.d(TAG, "Checking auth status with node: ${node.displayName}")
                    val authResult = messageClient.sendMessage(
                        node.id,
                        AUTH_CHECK_PATH,
                        byteArrayOf()
                    ).await()

                    Log.d(TAG, "Auth check response from ${node.displayName}: $authResult")
                }

                // Request sync data
                nodes.forEach { node ->
                    Log.d(TAG, "Requesting sync data from node: ${node.displayName}")
                    val result = messageClient.sendMessage(
                        node.id,
                        SYNC_PATH,
                        "sync_request".toByteArray()
                    ).await()

                    Log.d(TAG, "Sync request response from ${node.displayName}: $result")
                }

                // Listen for sync data
                val dataClient = Wearable.getDataClient(context)
                Log.d(TAG, "Waiting for sync data...")
                val dataItems = dataClient.dataItems.await()

                if (dataItems.count == 0) {
                    Log.d(TAG, "No sync data received")
                } else {
                    Log.d(TAG, "Received ${dataItems.count} data items")
                    dataItems.forEach { dataItem ->
                        Log.d(TAG, "Processing data item from ${dataItem.uri.host}")
                        if (dataItem.uri.path == SYNC_DATA_PATH) {
                            Log.d(TAG, "Received sync data from ${dataItem.uri.host}")
                            // Process the received data
                            val data = DataMapItem.fromDataItem(dataItem).dataMap
                            Log.d(TAG, "Sync data received: ${data.size()} items")

                            // Log all data items for debugging
                            data.keySet().forEach { key ->
                                Log.d(TAG, "Data item - $key: ${data.getString(key)}")
                            }
                        }
                    }
                }

                Log.d(TAG, "Sync process completed successfully")
                return username
            } catch (e: Exception) {
                Log.e(TAG, "Error during sync: ${e.message}")
                throw e
            }
        }
    }
}