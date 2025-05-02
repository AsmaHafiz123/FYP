package com.example.parental1

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parental1.model.SignupRequest
import com.example.parental1.model.SignupResponse
import com.example.parental1.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.parental1.network.SocketManager

class MainActivity : AppCompatActivity() {

    // Create an instance of SocketManager
    private val socketManager = SocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the socket connection
        socketManager.initializeSocket()


        // Listen for events (e.g., monitoring response)
        socketManager.onEvent("monitoring-response") { args ->
            // Handle the event when the parent receives a response from the child
            val parentEmail = args[0] as String
            val childEmail = args[1] as String
            val response = args[2] as String
            val imei = args[3] as String

            // You can update the UI or take actions based on the response here
            // Example: Show a Toast
            runOnUiThread {
                Toast.makeText(this, "Response: $response", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch the role from SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val role = sharedPref.getString("ROLE", null)

        if (role.isNullOrEmpty()) {
            // Role is not defined, redirect to RoleSelectionActivity
            Toast.makeText(this, "Please select a role first.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
            return
        }

        // Redirect to SignupActivity if role is available
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Handle signup and navigate to the next screen after successful signup
    private fun handleSignup(name: String, email: String, password: String, confirmPassword: String, role: String) {
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val signupRequest = SignupRequest(name.trim(), email.trim(), password.trim(), role)

        // Use the single signup endpoint
        val call = RetrofitClient.apiService.signup(signupRequest)

        // Enqueue the API call
        call.enqueue(object : Callback<SignupResponse> {
            override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                if (response.isSuccessful) {
                    val signupResponse = response.body()
                    if (signupResponse != null && signupResponse.success) {
                        Toast.makeText(this@MainActivity, "Signup Successful!", Toast.LENGTH_SHORT).show()

                        // After successful signup, navigate to the appropriate activity based on the role
                        val intent = if (role == "parent") {
                            Intent(this@MainActivity, VerificationActivity::class.java)
                        } else {
                            Intent(this@MainActivity, ChildMainActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Signup failed: ${signupResponse?.message ?: "Unknown error"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Signup failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
