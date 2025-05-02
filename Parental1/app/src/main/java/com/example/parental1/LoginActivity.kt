package com.example.parental1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parental1.network.RetrofitClient
import com.example.parental1.model.LoginRequest
import com.example.parental1.model.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private var userRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login) // Your login screen layout

        // Get the stored role from SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userRole = sharedPref.getString("ROLE", "parent") // Default to parent if no role is stored

        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.login_button)

        // Setting up the Sign Up and Forgot Password links
        val signupTextView = findViewById<TextView>(R.id.signup_text_view)
        val forgotPasswordTextView = findViewById<TextView>(R.id.forgot_password)

        // Set click listener for "Sign Up" link
        signupTextView.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignupActivity::class.java)
            startActivity(intent) // Navigate to SignUpActivity
        }

        // Set click listener for "Forgot Password?" link
        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this@LoginActivity, ForgotPasswordActivity::class.java)
            startActivity(intent) // Navigate to ForgotPasswordActivity
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                val loginRequest = LoginRequest(email, password, userRole ?: "parent")

                // If role is parent, check parent table; if role is child, check child table
                val checkEmailCall = if (userRole == "parent") {
                    RetrofitClient.apiService.checkEmailInParent(email)
                } else {
                    RetrofitClient.apiService.checkEmailInChild(email)
                }

                // This is where you're checking if the email exists in the parent or child table.
                checkEmailCall.enqueue(object : Callback<Boolean> {
                    override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                        if (response.isSuccessful) {
                            if (response.body() == true) {
                                // Email exists, proceed to check password
                                RetrofitClient.apiService.login(loginRequest)
                                    .enqueue(object : Callback<LoginResponse> {
                                        override fun onResponse(
                                            call: Call<LoginResponse>,
                                            response: Response<LoginResponse>
                                        ) {
                                            if (response.isSuccessful) {
                                                val loginResponse = response.body()
                                                if (loginResponse != null && loginResponse.success) {

                                                    // Save email and role to SharedPreferences
                                                    val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                                    with(sharedPref.edit()) {
                                                        putString("EMAIL", loginRequest.email)
                                                        putString("ROLE", loginRequest.role)
                                                        apply()
                                                    }

                                                    // Handle successful login
                                                    Toast.makeText(
                                                        applicationContext,
                                                        "Login Successful",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    // Redirect based on the stored role
                                                    val intent = if (userRole == "parent") {
                                                        Intent(
                                                            this@LoginActivity,
                                                            DashboardActivity::class.java
                                                        )
                                                    } else {
                                                        Intent(
                                                            this@LoginActivity,
                                                            ChildMainActivity::class.java
                                                        )
                                                    }

                                                    startActivity(intent)
                                                    finish() // Close LoginActivity so user can't navigate back
                                                } else {
                                                    // Incorrect password case
                                                    Toast.makeText(
                                                        applicationContext,
                                                        "Incorrect password. Go to Forgot Password?",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else {
                                                // Unauthorized or other error
                                                if (response.code() == 401) {
                                                    Toast.makeText(
                                                        applicationContext,
                                                        "Incorrect password. Go to Forgot Password?",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        applicationContext,
                                                        "Error: ${response.message()}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }

                                        override fun onFailure(
                                            call: Call<LoginResponse>,
                                            t: Throwable
                                        ) {
                                            Toast.makeText(
                                                applicationContext,
                                                "Network Error: ${t.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                            } else {
                                // Email does not exist, show message to sign up
                                Toast.makeText(
                                    applicationContext,
                                    "Don't have an account? Sign up",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Error: ${response.message()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<Boolean>, t: Throwable) {
                        Toast.makeText(
                            applicationContext,
                            "Network Error: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        }
    }
}
