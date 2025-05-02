package com.example.parental1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parental1.model.EmailRequest
import com.example.parental1.model.EmailResponse
import com.example.parental1.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val sendCodeButton = findViewById<Button>(R.id.resetPasswordButton)

        sendCodeButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            } else {
                // API call to send reset code
                val emailRequest = EmailRequest(email)
                val call = RetrofitClient.apiService.forgotPassword(emailRequest)

                call.enqueue(object : Callback<EmailResponse> {
                    override fun onResponse(call: Call<EmailResponse>, response: Response<EmailResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@ForgotPasswordActivity, "Reset code sent to your email", Toast.LENGTH_SHORT).show()

                            // Navigate to Reset Code Activity
                            val intent = Intent(this@ForgotPasswordActivity, ResetCodeActivity::class.java)
                            intent.putExtra("email", email) // Pass email to Reset Code Activity
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@ForgotPasswordActivity, "Failed to send reset code", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<EmailResponse>, t: Throwable) {
                        Toast.makeText(this@ForgotPasswordActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
}
