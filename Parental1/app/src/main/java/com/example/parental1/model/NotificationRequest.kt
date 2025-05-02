package com.example.parental1.model

data class NotificationRequest(
    val parent_email: String,
    val child_email: String,
    val request_status: String,  // For example, "pending" or "approved"
    val imei: String
)
