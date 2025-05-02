package com.example.parental1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.parental1.model.CheckEmailResponse
import com.example.parental1.model.ConnectionForRequest
import com.example.parental1.model.ConnectionForResponse
import com.example.parental1.model.DeviceConnectionRequest
import com.example.parental1.model.DeviceConnectionResponse
import com.example.parental1.network.RetrofitClient
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerificationActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST = 101

    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private lateinit var scanButton: Button
    private lateinit var connectionSuccessButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        // Initialize Views
        barcodeScannerView = findViewById(R.id.barcode_scanner)
        scanButton = findViewById(R.id.scan_button)
        connectionSuccessButton = findViewById(R.id.connectionSuccessTextView)

        // Initially hide the scanner and connection success button
        barcodeScannerView.visibility = View.GONE
        connectionSuccessButton.visibility = View.GONE

        // Start QR Code Scanner on Button Click
        scanButton.setOnClickListener {
            checkCameraPermissionAndStartScanner()
        }
    }

    private fun checkCameraPermissionAndStartScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanner()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        }
    }

    private fun startScanner() {
        scanButton.visibility = View.GONE
        barcodeScannerView.visibility = View.VISIBLE

        barcodeScannerView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (result.text != null) {
                    try {
                        val jsonObject = JSONObject(result.text)
                        val parentEmail = jsonObject.getString("parent_email")
                        val childEmail = jsonObject.getString("child_email")

                        // First verify the parent email
                        checkParentEmail(parentEmail, childEmail)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@VerificationActivity,
                            "Error parsing QR code: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        resetScanner()
                    }
                } else {
                    Toast.makeText(
                        this@VerificationActivity,
                        "Scan Failed. Try Again!",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetScanner()
                }
            }
        })
    }

    private fun resetScanner() {
        barcodeScannerView.pause()
        scanButton.visibility = View.VISIBLE
        barcodeScannerView.visibility = View.GONE
    }

    private fun checkParentEmail(parentEmail: String, childEmail: String) {
        val apiService = RetrofitClient.apiService

        apiService.checkEmailForQrCode(parentEmail).enqueue(object : Callback<CheckEmailResponse> {
            override fun onResponse(call: Call<CheckEmailResponse>, response: Response<CheckEmailResponse>) {
                if (response.isSuccessful) {
                    val emailExists = response.body()?.exists ?: false
                    if (emailExists) {
                        // First add device connection
                        addDeviceConnection(parentEmail, childEmail)
                    } else {
                        Toast.makeText(
                            this@VerificationActivity,
                            "Invalid parent email. Please ensure the QR code is valid.",
                            Toast.LENGTH_SHORT
                        ).show()
                        resetScanner()
                    }
                } else {
                    Toast.makeText(this@VerificationActivity, "Error verifying parent email", Toast.LENGTH_SHORT).show()
                    resetScanner()
                }
            }

            override fun onFailure(call: Call<CheckEmailResponse>, t: Throwable) {
                Toast.makeText(this@VerificationActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                resetScanner()
            }
        })
    }

    private fun addDeviceConnection(parentEmail: String, childEmail: String) {
        val deviceConnectionRequest = DeviceConnectionRequest(
            parent_email = parentEmail,
            child_email = childEmail
        )

        RetrofitClient.apiService.addDeviceConnection(deviceConnectionRequest)
            .enqueue(object : Callback<DeviceConnectionResponse> {
                override fun onResponse(
                    call: Call<DeviceConnectionResponse>,
                    response: Response<DeviceConnectionResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        // After successful device connection, verify the connection
                        verifyConnection(parentEmail, childEmail)
                    } else {
                        Toast.makeText(
                            this@VerificationActivity,
                            "Failed to record connection: ${response.body()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        resetScanner()
                    }
                }

                override fun onFailure(call: Call<DeviceConnectionResponse>, t: Throwable) {
                    Toast.makeText(
                        this@VerificationActivity,
                        "Network error while recording connection: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetScanner()
                }
            })
    }

    private fun verifyConnection(parentEmail: String, childEmail: String) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("JWT_TOKEN", null)

        if (token == null) {
            Toast.makeText(this, "Authentication error. Please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@VerificationActivity, LoginActivity::class.java))
            finish()
            return
        }

        val connectionRequest = ConnectionForRequest(
            parent_email = parentEmail,
            child_email = childEmail
        )

        // Add logging for debugging
        println("Sending verify-connection request with token: $token")
        println("Request body: $connectionRequest")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.verifyConnection(
                    "Bearer $token",
                    connectionRequest
                ).execute()

                withContext(Dispatchers.Main) {
                    when {
                        response.code() == 403 -> {
                            Toast.makeText(
                                this@VerificationActivity,
                                "Please use the same email as used during signup",
                                Toast.LENGTH_LONG
                            ).show()
                            resetScanner()
                        }
                        response.code() == 404 -> {
                            Toast.makeText(
                                this@VerificationActivity,
                                "No pending connection request found",
                                Toast.LENGTH_LONG
                            ).show()
                            resetScanner()
                        }
                        response.code() == 401 -> {
                            Toast.makeText(
                                this@VerificationActivity,
                                "Session expired. Please login again",
                                Toast.LENGTH_LONG
                            ).show()
                            startActivity(Intent(this@VerificationActivity, LoginActivity::class.java))
                            finish()
                        }
                        response.isSuccessful -> {
                            val responseBody = response.body()
                            if (responseBody?.success == true) {
                                // Save parent and child emails in SharedPreferences
                                val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putString("parent_email", parentEmail)
                                    putString("selected_child_email", childEmail) // Save as selected_child_email
                                    apply()
                                }

                                // Show success message
                                Toast.makeText(
                                    this@VerificationActivity,
                                    responseBody.message,
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Redirect to DashboardActivity
                                val intent = Intent(this@VerificationActivity, DashboardActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(intent)
                                finish() // Close VerificationActivity
                            } else {
                                Toast.makeText(
                                    this@VerificationActivity,
                                    "Verification failed: ${responseBody?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                resetScanner()
                            }
                        }
                        else -> {
                            Toast.makeText(
                                this@VerificationActivity,
                                "Error: ${response.message()}",
                                Toast.LENGTH_SHORT
                            ).show()
                            resetScanner()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@VerificationActivity,
                        "Network Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetScanner()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanner()
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show()
                // Optionally, guide the user to settings if they permanently deny the permission
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    Toast.makeText(
                        this,
                        "Please enable Camera permission in Settings to proceed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeScannerView.isInitialized && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeScannerView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeScannerView.isInitialized) {
            barcodeScannerView.pause()
        }
    }
}