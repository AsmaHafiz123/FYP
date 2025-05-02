package com.example.parental1.model

data class IMEIRequest(
    val imei: String,
    val targetDeviceId: String? = null // Optional, used if you want to notify the target device
)
