package com.example.myfindgo.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getScore(userId: String): Result<UserScore> {
        return try {
            val document = firestore.collection("scores")
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                Result.success(document.toObject(UserScore::class.java) ?: UserScore())
            } else {
                Result.success(UserScore(userId = userId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateScore(userId: String, score: Int): Result<Unit> {
        return try {
            val userScore = UserScore(userId = userId, score = score)
            firestore.collection("scores")
                .document(userId)
                .set(userScore)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser() = auth.currentUser
    fun signOut() = auth.signOut()
} 