package com.example.parental1

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.parental1.network.RetrofitClient
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MultiFeaturesJobService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ScreenCaptureChannel"
        private const val SCREENSHOT_INTERVAL_MS = 2000L // Capture every 30 seconds
        private const val SERVER_URL = "http://192.168.84.46:3000"
    }

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var handler: Handler? = null
    private var socket: Socket? = null
    private var childEmail: String = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        try {
            socket = IO.socket(SERVER_URL)
            socket?.connect()
            Log.d("SocketIO", "Socket initialized successfully")
        } catch (e: Exception) {
            Log.e("SocketIO", "Error initializing socket", e)
        }

       // val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        //childEmail = sharedPreferences.getString("child_email", "") ?: ""

        // Use "UserPrefs" to match ChildMainActivity
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        childEmail = sharedPreferences.getString("EMAIL", "") ?: "" // Fix key from "child_email" to "EMAIL"

        if (childEmail.isEmpty()) {
            Log.e("MultiFeaturesJobService", "Child email is empty, stopping service")
            Toast.makeText(this, "Child email not set, stopping monitoring", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        Log.d("MultiFeaturesJobService", "Child email retrieved: $childEmail")
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_BEST,
            time - 5 * 60 * 1000,
            time
        )
        if (stats.isNullOrEmpty()) {
            Log.w("MultiFeaturesJobService", "Usage stats permission not granted, app tracking disabled")
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Permission Required")
                .setContentText("Please enable Usage Access to track apps")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(2, notification)
        } else {
            Log.d("MultiFeaturesJobService", "Usage stats permission available")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MultiFeaturesJobService", "onStartCommand called with intent: $intent")
        val notification = buildNotification()
        // Start as foreground service with media projection type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val resultCode = intent?.getIntExtra("result_code", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("media_projection_data")
        Log.d("MultiFeaturesJobService", "resultCode: $resultCode, data: $data")

        if (resultCode == Activity.RESULT_OK && data != null) {
            Log.d("MultiFeaturesJobService", "Starting screen capture")
            startScreenCapture(resultCode, data)
        } else {
            Log.e("MultiFeaturesJobService", "Invalid intent data: resultCode=$resultCode, data=$data")
            Toast.makeText(this, "Invalid screen capture data", Toast.LENGTH_SHORT).show()
            stopSelf()
        }

        return START_STICKY
    }

    private fun startScreenCapture(resultCode: Int, data: Intent) {
        Log.d("MultiFeaturesJobService", "startScreenCapture called")
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            if (mediaProjection == null) {
                Log.e("MultiFeaturesJobService", "Failed to initialize MediaProjection")
                stopSelf()
                return
            }

            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            Log.d("MultiFeaturesJobService", "Display metrics: width=$width, height=$height, density=$density")

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            Log.d("MultiFeaturesJobService", "ImageReader created")
            mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null
            )

            val handlerThread = HandlerThread("ScreenCaptureThread")
            handlerThread.start()
            handler = Handler(handlerThread.looper)
            handler?.postDelayed(object : Runnable {
                override fun run() {
                    Log.d("MultiFeaturesJobService", "Attempting screenshot capture")
                    captureAndSendScreenshot()
                    handler?.postDelayed(this, SCREENSHOT_INTERVAL_MS)
                }
            }, SCREENSHOT_INTERVAL_MS)
        } catch (e: Exception) {
            Log.e("MultiFeaturesJobService", "Error starting screen capture: ${e.message}", e)
            stopSelf()
        }
    }

    fun captureAndSendScreenshot() {
        Log.d("MultiFeaturesJobService", "captureAndSendScreenshot called")
        var image: Image? = null
        try {
            image = imageReader?.acquireLatestImage()
            if (image != null) {
                Log.d("MultiFeaturesJobService", "Image acquired: width=${image.width}, height=${image.height}")
                val width = image.width
                val height = image.height
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                val resizedBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    800,
                    (bitmap.height * 800f / bitmap.width).toInt(),
                    true
                )
                val byteArrayOutputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val screenshotBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)

                Log.d("ScreenCapture", "Base64 size: ${screenshotBase64.length / 1024} KB")

                bitmap.recycle()
                resizedBitmap.recycle()

                val screenData = mapOf(
                    "child_email" to childEmail,
                    "screenshot" to screenshotBase64,
                    "current_app" to getCurrentApp(),
                    "created_at" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )

                sendViaRetrofit(screenData)

                if (socket?.connected() == true) {
                    sendViaSocket(screenData)
                }

                Log.d("ScreenCapture", "Screenshot captured and sent successfully")
            } else {
                Log.w("ScreenCapture", "No image available to capture")
            }
        } catch (e: Exception) {
            Log.e("ScreenCapture", "Error capturing/sending screenshot: ${e.message}", e)
        } finally {
            image?.close()
        }
    }

    private fun sendViaRetrofit(screenData: Map<String, String>) {
        val call: Call<Unit> = RetrofitClient.apiService.sendScreenData(screenData)

        CoroutineScope(Dispatchers.IO).launch {
            var retries = 3
            while (retries > 0) {
                try {
                    val response = call.clone().execute()
                    if (response.isSuccessful) {
                        Log.d("MultiFeaturesJobService", "Screen data sent successfully")
                        return@launch
                    } else {
                        Log.e("MultiFeaturesJobService", "Failed to send screen data: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("MultiFeaturesJobService", "Error sending screen data: ${e.message}")
                }
                retries--
                if (retries > 0) {
                    Log.d("MultiFeaturesJobService", "Retrying... ($retries attempts left)")
                    delay(5000)
                } else {
                    Log.e("MultiFeaturesJobService", "All retries failed")
                }
            }
        }
    }

    private fun sendViaSocket(screenData: Map<String, String>) {
        socket?.emit("screen_data", JSONObject(screenData))
        Log.d("MultiFeaturesJobService", "Screen data sent via socket")
    }

    private fun getCurrentApp(): String {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_BEST,
            time - 5 * 60 * 1000, // Last 5 minutes
            time
        )
        if (stats.isNullOrEmpty()) {
            Log.w("MultiFeaturesJobService", "No usage stats available, check Usage Access permission")
            return "Unknown"
        }
        val latestApp = stats.filter {
            it.lastTimeUsed > time - 60 * 1000 && it.totalTimeInForeground > 0
        }.maxByOrNull { it.lastTimeUsed }
        return if (latestApp != null) {
            Log.d("MultiFeaturesJobService", "Foreground app: ${latestApp.packageName}, last used: ${latestApp.lastTimeUsed}")
            latestApp.packageName
        } else {
            Log.w("MultiFeaturesJobService", "No recent foreground app found")
            "Unknown"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        imageReader?.close()
        handler?.removeCallbacksAndMessages(null)
        socket?.disconnect()
        Log.d("MultiFeaturesJobService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Screen Capture Service"
            val descriptionText = "Monitoring active"
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Parental Monitoring")
            .setContentText("Monitoring child's device in the background")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .setSound(null)
            .setVibrate(null)
            .build()
    }
}

