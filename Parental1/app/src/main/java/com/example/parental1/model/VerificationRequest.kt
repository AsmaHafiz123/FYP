package com.example.parental1.model

data class VerificationRequest(
    val parentEmail: String,
    val childEmail: String,
    val imei: String
)
