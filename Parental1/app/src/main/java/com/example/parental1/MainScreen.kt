package com.example.parental1

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onNavigateToVerification: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSignup: (String, String) -> Unit // Updated to accept name and email as parameters
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to Parental Control App")

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { onSignup(name, email) }) {  // Pass name and email to the onSignup function
            Text(text = "Sign Up")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToVerification) {
            Text(text = "Go to Verification")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onNavigateToForgotPassword) {
            Text(text = "Forgot Password")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onNavigateToSignup) {
            Text(text = "Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onNavigateToLogin) {
            Text(text = "Login")
        }
    }
}
