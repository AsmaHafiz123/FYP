package com.example.parental1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parental1.model.UpdatePasswordRequest
import com.example.parental1.model.UpdatePasswordResponse
import com.example.parental1.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpdatePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.update_password)

        val newPasswordInput = findViewById<EditText>(R.id.newPasswordEditText)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordEditText)
        val updateButton = findViewById<Button>(R.id.updatePasswordButton)

        // Getting the email and resetCode from intent extras
        val email = intent.getStringExtra("email") ?: ""
        val resetCode = intent.getStringExtra("resetCode") ?: ""  // Added resetCode here

        updateButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                // Create the request object including the resetCode
                val updatePasswordRequest = UpdatePasswordRequest(email, newPassword, resetCode)

                // API call to update password
                val call = RetrofitClient.apiService.updatePassword(updatePasswordRequest)

                call.enqueue(object : Callback<UpdatePasswordResponse> {
                    override fun onResponse(call: Call<UpdatePasswordResponse>, response: Response<UpdatePasswordResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            // Password updated successfully
                            Toast.makeText(this@UpdatePasswordActivity, "Password updated successfully", Toast.LENGTH_SHORT).show()

                            // Redirect to login page after successful password update
                            val intent = Intent(this@UpdatePasswordActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish() // Close the UpdatePasswordActivity so user can't navigate back
                        } else {
                            Toast.makeText(this@UpdatePasswordActivity, "Failed to update password", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<UpdatePasswordResponse>, t: Throwable) {
                        Toast.makeText(this@UpdatePasswordActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
}
