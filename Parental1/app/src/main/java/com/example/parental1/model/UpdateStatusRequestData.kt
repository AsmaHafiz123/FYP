package com.example.parental1.model

data class UpdateRequestStatusData(
    val parent_email: String,
    val child_email: String,
    val imei: String,
    val response_status: String
)
