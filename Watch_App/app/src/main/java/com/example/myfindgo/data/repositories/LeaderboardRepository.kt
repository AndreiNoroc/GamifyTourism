package com.example.myfindgo.data.repositories

import com.example.myfindgo.data.models.LeaderboardEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class LeaderboardRepository {
    suspend fun getLeaderboard(): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        val url = URL("https://gamifytourism-955657412481.europe-west9.run.app/leaderboard")
        val json = url.readText()
        // Using kotlinx.serialization would be better, but for simplicity we'll parse manually
        json.trim('[', ']')
            .split("},{")
            .map { entryJson ->
                val properties = entryJson.trim('{', '}')
                    .split(",")
                    .associate { 
                        val (key, value) = it.split(":")
                        key.trim('"') to value.trim('"')
                    }
                
                LeaderboardEntry(
                    username = properties["username"] ?: "",
                    score = (properties["score"] ?: "0").toInt()
                )
            }
            .sortedByDescending { it.score }
    }
} 