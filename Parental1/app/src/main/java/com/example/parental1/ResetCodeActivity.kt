package com.example.parental1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parental1.model.VerifyCodeRequest
import com.example.parental1.model.VerifyCodeResponse
import com.example.parental1.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetCodeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reset_code)

        val codeInput = findViewById<EditText>(R.id.reset_code_input)
        val verifyButton = findViewById<Button>(R.id.verify_button)

        val email = intent.getStringExtra("email") ?: ""

        verifyButton.setOnClickListener {
            val resetCode = codeInput.text.toString().trim()

            if (resetCode.isEmpty()) {
                Toast.makeText(this, "Please enter the reset code", Toast.LENGTH_SHORT).show()
            } else {
                // API call to verify reset code
                val verifyCodeRequest = VerifyCodeRequest(email, resetCode)
                val call = RetrofitClient.apiService.verifyResetCode(verifyCodeRequest)

                call.enqueue(object : Callback<VerifyCodeResponse> {
                    override fun onResponse(call: Call<VerifyCodeResponse>, response: Response<VerifyCodeResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@ResetCodeActivity, "Code verified", Toast.LENGTH_SHORT).show()

                            // Navigate to Update Password Activity
                            val intent = Intent(this@ResetCodeActivity, UpdatePasswordActivity::class.java)
                            intent.putExtra("email", email) // Pass email to Update Password Activity
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@ResetCodeActivity, "Invalid reset code", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<VerifyCodeResponse>, t: Throwable) {
                        Toast.makeText(this@ResetCodeActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
}
