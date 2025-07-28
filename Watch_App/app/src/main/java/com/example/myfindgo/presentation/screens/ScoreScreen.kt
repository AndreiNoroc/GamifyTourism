package com.example.myfindgo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.myfindgo.data.UserRepository
import com.example.myfindgo.data.UserScore
import kotlinx.coroutines.launch

@Composable
fun ScoreScreen(
    userRepository: UserRepository,
    modifier: Modifier = Modifier
) {
    var score by remember { mutableStateOf<UserScore?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val currentUser = userRepository.getCurrentUser()
        if (currentUser != null) {
            scope.launch {
                userRepository.getScore(currentUser.uid)
                    .fold(
                        onSuccess = { userScore ->
                            score = userScore
                            isLoading = false
                        },
                        onFailure = {
                            errorMessage = it.message
                            isLoading = false
                        }
                    )
            }
        } else {
            errorMessage = "User not logged in"
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            score?.let { userScore ->
                Text("Your Score", style = MaterialTheme.typography.title1)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    userScore.score.toString(),
                    style = MaterialTheme.typography.display1
                )
            }
            
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colors.error
                )
            }
        }
    }
} 