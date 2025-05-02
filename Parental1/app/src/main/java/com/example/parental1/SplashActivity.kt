package com.example.parental1

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userRole = sharedPreferences.getString("ROLE", null) // Check if role is already set

        // Delay to show splash screen (2 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            if (userRole.isNullOrEmpty()) {
                // If no role is selected, navigate to RoleSelectionActivity
                val intent = Intent(this, RoleSelectionActivity::class.java)
                startActivity(intent)
            } else {
                // If role is already selected, navigate to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            finish() // Close SplashActivity
        }, 2000) // 2000 ms delay
    }
}
