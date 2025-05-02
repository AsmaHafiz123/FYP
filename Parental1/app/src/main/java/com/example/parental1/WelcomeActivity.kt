package com.example.parental1

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parental1.ui.theme.Parental1Theme

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve role from SharedPreferences
        val sharedPref: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val role = sharedPref.getString("ROLE", null)
        setContent {
            Parental1Theme {
                // Calling the WelcomeScreen composable and passing click listeners
                WelcomeScreen(
                    role = role,  // Pass role directly
                    onSignUpClick = {
                        val intent = Intent(this, SignupActivity::class.java)
                        startActivity(intent)
                    },
                    onLoginClick = {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}





@Composable
fun WelcomeScreen(role: String?,onSignUpClick: () -> Unit, onLoginClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF000F47), Color(0xFF00C977)) // Gradient from #000F47 to #00C977
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Add Image composable for the logo
            Image(
                painter = painterResource(id = R.drawable.logo),  // Replace "logo" with the name of your image
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 32.dp)
            )

            // Welcome text
            Text(
                text = "Welcome to Parental Patrol",
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Sign Up Button
            Button(
                onClick = onSignUpClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 16.dp)
            ) {
                Text(text = "Sign Up", color = Color.White)
            }

            // Login Button
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
            ) {
                Text(text = "Login", color = Color.White)
            }
        }
    }
}
