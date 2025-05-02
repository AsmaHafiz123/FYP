package com.example.parental1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view to the layout for role selection
        setContentView(R.layout.activity_role_selection)

        val parentButton: Button = findViewById(R.id.btnParent)
        val childButton: Button = findViewById(R.id.btnChild)

        // When Parent button is clicked, save role and navigate to WelcomeActivity
        parentButton.setOnClickListener {
            saveRoleAndNavigate("parent")
        }

        // When Child button is clicked, save role and navigate to WelcomeActivity
        childButton.setOnClickListener {
            saveRoleAndNavigate("child")
        }
    }

    // Save the selected role in SharedPreferences and navigate to WelcomeActivity
    private fun saveRoleAndNavigate(role: String) {
        // Save the selected role in SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("ROLE", role) // Save the selected role
        editor.apply()

        // Show a welcome message based on the selected role
        showWelcomeMessage(role)

        // Navigate to the WelcomeActivity and pass the role
        navigateToWelcomePage(role)
    }

    private fun showWelcomeMessage(role: String) {
        // Show the respective welcome message
        val welcomeMessage = if (role == "parent") {
            "Welcome in Parent App"
        } else {
            "Welcome in Child App"
        }
        Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToWelcomePage(role: String) {
        // Navigate to WelcomeActivity and pass the role
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.putExtra("ROLE", role) // Pass the role to WelcomeActivity
        startActivity(intent)
        finish() // Close RoleSelectionActivity so the user can't go back
    }
}
