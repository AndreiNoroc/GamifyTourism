package com.example.myfindgo.data

data class UserScore(
    val userId: String = "",
    val score: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)