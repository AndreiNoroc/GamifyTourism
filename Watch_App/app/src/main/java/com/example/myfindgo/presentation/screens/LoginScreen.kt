package com.example.myfindgo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.myfindgo.data.UserRepository
import kotlinx.coroutines.launch
import androidx.wear.compose.material.Text as Text1


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    userRepository: UserRepository
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text1("Welcome to myFindgo")
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text1("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text1("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    // Simulate a short delay to show loading state
                    kotlinx.coroutines.delay(500)
                    isLoading = false
                    onLoginSuccess()
                }
            },
            enabled = !isLoading
        ) {
            Text1(if (isLoading) "Loading..." else "Login")
        }
    }
}