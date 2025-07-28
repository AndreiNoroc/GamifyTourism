package com.example.myfindgo.data.api

import retrofit2.http.Body
import retrofit2.http.POST

data class VisitedLocationsResponse(
    val visitedLocations: List<String>
)

interface VisitedLocationsApi {
    @POST("/get-visited-locations")
    suspend fun getVisitedLocations(@Body request: Map<String, String>): VisitedLocationsResponse
} 