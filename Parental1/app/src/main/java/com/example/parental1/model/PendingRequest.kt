package com.example.parental1.model

data class PendingRequest(
    val parent_email: String,
    val imei: String,
    val status: String // This is the status of the request, e.g., "pending".
)
