package com.example.parental1.model

data class RequestData(
val parent_email: String,
val imei: String,
val request_status: String // Added String type for request_status
)