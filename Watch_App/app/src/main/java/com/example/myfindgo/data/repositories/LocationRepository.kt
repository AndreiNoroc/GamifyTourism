package com.example.myfindgo.data.repositories

import com.example.myfindgo.data.models.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class LocationRepository {
    suspend fun getLocations(): List<Location> = withContext(Dispatchers.IO) {
        val url = URL("https://gamifytourism-955657412481.europe-west9.run.app/locations")
        val json = url.readText()
        // Using kotlinx.serialization would be better, but for simplicity we'll parse manually
        json.trim('[', ']')
            .split("},{")
            .map { locationJson ->
                val properties = locationJson.trim('{', '}')
                    .split(",")
                    .associate { 
                        val (key, value) = it.split(":")
                        key.trim('"') to value.trim('"')
                    }
                
                Location(
                    _id = properties["_id"] ?: "",
                    name = properties["name"] ?: "",
                    latitude = (properties["latitude"] ?: "0.0").toDouble(),
                    longitude = (properties["longitude"] ?: "0.0").toDouble(),
                    score = (properties["score"] ?: "0").toInt()
                )
            }
    }
} 