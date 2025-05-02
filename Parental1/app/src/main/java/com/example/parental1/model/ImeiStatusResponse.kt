package com.example.parental1.model

// Data class for the response
data class ImeiStatusResponse(
    val success: Boolean,
    val message: String,
    val imei_verified: Boolean
)