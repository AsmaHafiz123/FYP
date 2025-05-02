package com.example.parental1.model

data class ChildResponse(
    val child_email: String,
    val imei: String,
    val response_status: String // "accepted" or "rejected"
)
