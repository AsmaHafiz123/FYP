package com.example.parental1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.parental1.model.*
import com.example.parental1.network.RetrofitClient
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicBoolean

class ChildMainActivity : AppCompatActivity() {

    private lateinit var childEmailEditText: EditText
    private lateinit var parentEmailEditText: EditText
    private lateinit var generateQRButton: Button
    private lateinit var qrCodeImageView: ImageView
    private lateinit var connectionSuccessTextView: TextView
    private var childEmail: String = ""
    private var pollingJob: Job? = null
    private val isPolling = AtomicBoolean(false)
    private var socket: Socket? = null
    private val MEDIA_PROJECTION_REQUEST_CODE = 100

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.PACKAGE_USAGE_STATS,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_PHONE_STATE
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    private var currentPermissionIndex = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            currentPermissionIndex++
            requestNextPermission()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private val specialPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (checkSpecialPermission()) {
            currentPermissionIndex++
            requestNextPermission()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_main)

        // Fetch child email from SharedPreferences (saved during login)
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        childEmail = sharedPreferences.getString("EMAIL", "") ?: ""
        if (childEmail.isEmpty()) {
            Log.e("ChildMainActivity", "Child email is empty")
            Toast.makeText(this, "Child email not set. Please log in again.", Toast.LENGTH_LONG).show()
        }

        initializeViews()
        setupClickListeners()
        initializeSocket()
    }

    private fun initializeSocket() {
        try {
            val options = IO.Options()
            socket = IO.socket("http://192.168.84.46:3000")
            socket?.connect()

            if (childEmail.isNotEmpty()) {
                socket?.emit("register_child", childEmail)
            } else {
                Log.w("SocketIO", "Child email empty, skipping registration")
            }

            socket?.on("connection_success") {
                runOnUiThread {
                    showConnectionSuccess()
                }
            }

            Log.d("SocketIO", "Socket initialized successfully")
        } catch (e: Exception) {
            Log.e("SocketIO", "Error initializing socket", e)
            Toast.makeText(this, "Socket connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showConnectionSuccess() {
        val parentEmail = parentEmailEditText.text.toString().trim()
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("connected_parent_email", parentEmail).apply()

        connectionSuccessTextView.visibility = View.VISIBLE
        connectionSuccessTextView.text = "Connected Successfully! Monitoring is running in the background."
        qrCodeImageView.visibility = View.GONE
        generateQRButton.visibility = View.GONE
        childEmailEditText.visibility = View.GONE
        parentEmailEditText.visibility = View.GONE

        startScreenCapture()
        Log.d("ChildMainActivity", "Connection success shown, monitoring started")
    }

    private fun startScreenCapture() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, MEDIA_PROJECTION_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("ChildMainActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode, data=$data")
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    val serviceIntent = Intent(this, MultiFeaturesJobService::class.java).apply {
                        putExtra("result_code", resultCode)
                        putExtra("media_projection_data", data)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    Log.d("ChildMainActivity", "MultiFeaturesJobService started successfully")
                } catch (e: Exception) {
                    Log.e("ChildMainActivity", "Error starting service: ${e.message}", e)
                    Toast.makeText(this, "Failed to start monitoring: ${e.message}", Toast.LENGTH_LONG).show()
                    connectionSuccessTextView.visibility = View.GONE
                    qrCodeImageView.visibility = View.VISIBLE
                }
            } else {
                Log.e("ChildMainActivity", "Screen capture permission denied")
                Toast.makeText(this, "Screen capture permission denied. Monitoring cannot start.", Toast.LENGTH_LONG).show()
                connectionSuccessTextView.visibility = View.GONE
                qrCodeImageView.visibility = View.VISIBLE
            }
        }
    }

    private fun initializeViews() {
        childEmailEditText = findViewById(R.id.childEmailEditText)
        parentEmailEditText = findViewById(R.id.parentEmailEditText)
        generateQRButton = findViewById(R.id.generateQRButton)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        connectionSuccessTextView = findViewById(R.id.connectionSuccessTextView)

        childEmailEditText.setText(childEmail)
        childEmailEditText.isEnabled = false
        connectionSuccessTextView.visibility = TextView.GONE
    }

    private fun setupClickListeners() {
        generateQRButton.setOnClickListener {
            val parentEmail = parentEmailEditText.text.toString().trim()
            if (parentEmail.isNotEmpty()) {
                currentPermissionIndex = 0
                requestNextPermission()
            } else {
                Toast.makeText(this, "Please enter a valid parent email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestNextPermission() {
        if (currentPermissionIndex >= requiredPermissions.size) {
            val parentEmail = parentEmailEditText.text.toString().trim()
            checkParentEmail(parentEmail)
            return
        }

        val permission = requiredPermissions[currentPermissionIndex]

        if (permission == Manifest.permission.PACKAGE_USAGE_STATS ||
            permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE) {
            requestSpecialPermission(permission)
            return
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            currentPermissionIndex++
            requestNextPermission()
        } else {
            showPermissionRationaleDialog(permission)
        }
    }

    private fun showPermissionRationaleDialog(permission: String) {
        val permissionName = getPermissionFriendlyName(permission)

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("To monitor and protect your device, we need access to $permissionName. This helps your parent ensure your online safety.")
            .setPositiveButton("Grant") { _, _ ->
                requestPermissionLauncher.launch(permission)
            }
            .setNegativeButton("Not Now") { _, _ ->
                showPermissionDeniedDialog()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("This app requires all permissions to function properly. Without these permissions, we cannot generate the QR code for connection with your parent.")
            .setPositiveButton("Try Again") { _, _ ->
                requestNextPermission()
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(
                    this,
                    "QR code generation cancelled. All permissions are required.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestSpecialPermission(permission: String) {
        val permissionName = getPermissionFriendlyName(permission)

        AlertDialog.Builder(this)
            .setTitle("Special Permission Required")
            .setMessage("To monitor and protect your device, we need access to $permissionName. You'll be redirected to a settings screen to enable this permission.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = when (permission) {
                    Manifest.permission.PACKAGE_USAGE_STATS -> {
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    }
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        }
                    }
                    else -> {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    }
                }
                specialPermissionLauncher.launch(intent)
                if (permission == Manifest.permission.PACKAGE_USAGE_STATS) {
                    Toast.makeText(this, "Please enable Usage Access for accurate app tracking", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Not Now") { _, _ ->
                showPermissionDeniedDialog()
            }
            .setCancelable(false)
            .show()
    }

    private fun checkSpecialPermission(): Boolean {
        val permission = requiredPermissions[currentPermissionIndex]

        return when (permission) {
            Manifest.permission.PACKAGE_USAGE_STATS -> {
                val appOpsManager = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOpsManager.unsafeCheckOpNoThrow(
                        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(),
                        packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOpsManager.checkOpNoThrow(
                        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(),
                        packageName
                    )
                }
                if (mode == android.app.AppOpsManager.MODE_ALLOWED) {
                    Log.d("ChildMainActivity", "Usage stats permission granted")
                    true
                } else {
                    Log.w("ChildMainActivity", "Usage stats permission not granted")
                    Toast.makeText(this, "Please enable Usage Access in Settings to track apps", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    startActivity(intent)
                    false
                }
            }
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                }
            }
            else -> false
        }
    }

    private fun getPermissionFriendlyName(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera"
            Manifest.permission.ACCESS_FINE_LOCATION -> "Precise Location"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Approximate Location"
            Manifest.permission.READ_CONTACTS -> "Contacts"
            Manifest.permission.WRITE_CONTACTS -> "Contacts"
            Manifest.permission.RECORD_AUDIO -> "Microphone"
            Manifest.permission.READ_CALL_LOG -> "Call Logs"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage"
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> "File Management"
            Manifest.permission.PACKAGE_USAGE_STATS -> "App Usage Data"
            Manifest.permission.READ_MEDIA_IMAGES -> "Photos & Images"
            Manifest.permission.READ_MEDIA_VIDEO -> "Videos"
            Manifest.permission.READ_MEDIA_AUDIO -> "Audio Files"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            Manifest.permission.ANSWER_PHONE_CALLS -> "Phone Calls"
            Manifest.permission.READ_PHONE_STATE -> "Phone Status"
            else -> "this feature"
        }
    }

    private fun checkParentEmail(parentEmail: String) {
        val appOpsManager = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
            Log.w("ChildMainActivity", "Usage stats permission required before QR code")
            Toast.makeText(this, "Please enable Usage Access in Settings", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }

        val apiService = RetrofitClient.apiService
        val call = apiService.checkEmailForQrCode(parentEmail)
        call.enqueue(object : Callback<CheckEmailResponse> {
            override fun onResponse(call: Call<CheckEmailResponse>, response: Response<CheckEmailResponse>) {
                if (response.isSuccessful && response.body()?.exists == true) {
                    generateQRCode(parentEmail)
                } else {
                    Log.e("ChildMainActivity", "Parent email check failed: ${response.code()} - ${response.message()}")
                    Toast.makeText(
                        this@ChildMainActivity,
                        "Parent email not found. Please ensure this email exists as parent.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<CheckEmailResponse>, t: Throwable) {
                Log.e("ChildMainActivity", "Network error checking parent email: ${t.message}")
                Toast.makeText(this@ChildMainActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun generateQRCode(parentEmail: String) {
        val apiService = RetrofitClient.apiService
        val combinedEmails = "$parentEmail,$childEmail"

        CoroutineScope(Dispatchers.IO).launch {
            val call = apiService.generateQRCode(combinedEmails)
            try {
                val response = call.execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val qrResponse = response.body()
                        if (qrResponse?.success == true) {
                            displayQRCode(qrResponse.qrCodeUrl)
                            addDeviceConnection(parentEmail)
                        } else {
                            Log.e("ChildMainActivity", "QR code generation failed: ${response.message()}")
                            Toast.makeText(this@ChildMainActivity, "Error: QR Code not generated", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("ChildMainActivity", "QR code server error: ${response.code()} - ${response.message()}")
                        Toast.makeText(this@ChildMainActivity, "Server error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ChildMainActivity", "Network error generating QR code: ${e.message}")
                    Toast.makeText(this@ChildMainActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayQRCode(qrCodeUrl: String) {
        try {
            val qrCodeBytes = Base64.decode(qrCodeUrl.split(",")[1], Base64.DEFAULT)
            val qrCodeBitmap = BitmapFactory.decodeByteArray(qrCodeBytes, 0, qrCodeBytes.size)

            qrCodeImageView.setImageBitmap(qrCodeBitmap)
            qrCodeImageView.visibility = ImageView.VISIBLE

            generateQRButton.visibility = Button.GONE
            childEmailEditText.visibility = EditText.GONE
            parentEmailEditText.visibility = EditText.GONE
        } catch (e: Exception) {
            Log.e("ChildMainActivity", "Error displaying QR code: ${e.message}")
            Toast.makeText(this, "Failed to display QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addDeviceConnection(parentEmail: String) {
        val apiService = RetrofitClient.apiService
        val deviceConnectionRequest = DeviceConnectionRequest(
            parent_email = parentEmail,
            child_email = childEmail
        )

        val call = apiService.addDeviceConnection(deviceConnectionRequest)
        call.enqueue(object : Callback<DeviceConnectionResponse> {
            override fun onResponse(
                call: Call<DeviceConnectionResponse>,
                response: Response<DeviceConnectionResponse>
            ) {
                if (response.isSuccessful) {
                    Log.d("ChildMainActivity", "Device connection added successfully")
                    Toast.makeText(
                        this@ChildMainActivity,
                        "Connection request sent successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.e("ChildMainActivity", "Failed to add device connection: ${response.code()} - ${response.message()}")
                    Toast.makeText(
                        this@ChildMainActivity,
                        "Failed to record connection",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<DeviceConnectionResponse>, t: Throwable) {
                Log.e("ChildMainActivity", "Network error adding device connection: ${t.message}")
                Toast.makeText(
                    this@ChildMainActivity,
                    "Network error while recording connection: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        Log.d("ChildMainActivity", "/Activity started")
    }

    override fun onStop() {
        super.onStop()
        Log.d("ChildMainActivity", "Activity stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ChildMainActivity", "Activity destroyed")
        isPolling.set(false)
        pollingJob?.cancel()
        socket?.disconnect()
    }
}