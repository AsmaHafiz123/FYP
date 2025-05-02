package com.example.parental1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parental1.model.SignupRequest
import com.example.parental1.model.SignupResponse
import com.example.parental1.network.ApiService
import com.example.parental1.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Patterns

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        val signUpButton = findViewById<Button>(R.id.signup_button)
        val nameEditText = findViewById<EditText>(R.id.name)
        val emailEditText = findViewById<EditText>(R.id.email_edit_text)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirm_button)

        // Fetch role from SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val role = sharedPref.getString("ROLE", null)

        // Ensure that the role is selected, otherwise return
        if (role == null) {
            Toast.makeText(this, "Please select a role first.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        signUpButton.setOnClickListener {
            val username = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // Validate input fields
            if (!validateInputs(username, email, password, confirmPassword)) return@setOnClickListener

            // Create the signup request
            val signupRequest = SignupRequest(username, email, password, role)

            // Initialize apiService
            val apiService = RetrofitClient.apiService

            // Check if the email already exists in either parent or child table before proceeding
            checkEmailExists(email, signupRequest)
        }

        // Switch to LoginActivity if already registered
        val loginTextView: TextView = findViewById(R.id.login_text_view)
        loginTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validateInputs(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        return when {
            username.isEmpty() -> {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                false
            }
            email.isEmpty() -> {
                Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                false
            }
            password.isEmpty() -> {
                Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show()
                false
            }
            password != confirmPassword -> {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }


    /*    private fun checkEmailExists(email: String, signupRequest: SignupRequest) {
            val apiService = RetrofitClient.apiService

            // Check email in parent table
            val parentCall = apiService.checkEmailInParent(email)
            parentCall.enqueue(object : Callback<Boolean> {
                override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                    if (response.isSuccessful && response.body() == true) {
                        // Show error message if email exists in parent table
                        showError("Email already exists in the parent table")
                    } else {
                        // Check email in child table
                        checkEmailInChildTable(email, signupRequest)
                    }
                }

                override fun onFailure(call: Call<Boolean>, t: Throwable) {
                    // Handle failure (e.g., network error)
                    showError("Network error: Unable to check parent email")
                }
            })
        }

        private fun checkEmailInChildTable(email: String, signupRequest: SignupRequest) {
            val apiService = RetrofitClient.apiService

            // Check email in child table
            val childCall = apiService.checkEmailInChild(email)
            childCall.enqueue(object : Callback<Boolean> {
                override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                    if (response.isSuccessful && response.body() == true) {
                        // Show error message if email exists in child table
                        showError("Email already exists in the child table")
                    } else {
                        // If email is available, proceed with signup
                        performSignup(signupRequest)
                    }
                }
                override fun onFailure(call: Call<Boolean>, t: Throwable) {
                    // Handle failure (e.g., network error)
                    showError("Network error: Unable to check child email")
                }
            })
        }
    */

    private fun checkEmailExists(email: String, signupRequest: SignupRequest) {
        val apiService = RetrofitClient.apiService

        // Check email in parent table first
        val parentCall = apiService.checkEmailInParent(email)
        parentCall.enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful && response.body() == true) {
                    // If email exists in parent table
                    if (signupRequest.role == "parent") {
                        showError("Already have an account. Please login.")
                    } else {
                        showError("Already used this email as a parent.")
                    }
                } else {
                    // If not found in parent table, check child table
                    checkEmailInChildTable(email, signupRequest)
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                showError("Network error: Unable to check parent email")
            }
        })
    }

    private fun checkEmailInChildTable(email: String, signupRequest: SignupRequest) {
        val apiService = RetrofitClient.apiService

        // Check email in child table
        val childCall = apiService.checkEmailInChild(email)
        childCall.enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful && response.body() == true) {
                    // If email exists in child table
                    if (signupRequest.role == "child") {
                        showError("Already have an account. Please login.")
                    } else {
                        showError("Already used this email as a child.")
                    }
                } else {
                    // If email is not found in either table, proceed with signup
                    performSignup(signupRequest)
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                showError("Network error: Unable to check child email")
            }
        })
    }












    private fun performSignup(signupRequest: SignupRequest) {
        val apiService = RetrofitClient.apiService
        val call = apiService.signup(signupRequest)

        call.enqueue(object : Callback<SignupResponse> {
            override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                if (response.isSuccessful) {
                    val signupResponse = response.body()
                    if (signupResponse?.success == true) {

                        // Save email, role, and token to SharedPreferences
                        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("EMAIL", signupResponse.user?.email)
                            putString("ROLE", signupResponse.user?.role)
                            putString("JWT_TOKEN", signupResponse.token) // Save the token
                            apply() // Use apply() for asynchronous save
                        }


                        Toast.makeText(
                            this@SignupActivity,
                            "Signup successful: ${signupResponse.message}",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to the next activity based on the role
                        navigateToNextActivity(signupRequest.role)
                    } else {
                        Toast.makeText(
                            this@SignupActivity,
                            "Signup failed: ${signupResponse?.message ?: "Unknown error"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@SignupActivity,
                        "Signup failed: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                Toast.makeText(
                    this@SignupActivity,
                    "Signup failed: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun navigateToNextActivity(role: String) {
        val intent = if (role == "parent") {
            Intent(this@SignupActivity, VerificationActivity::class.java)  // For parent
        } else {
            Intent(this@SignupActivity, ChildMainActivity::class.java)     // For child
        }
        startActivity(intent)
        finish() // Close current activity
    }

    // Function to display error messages
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
