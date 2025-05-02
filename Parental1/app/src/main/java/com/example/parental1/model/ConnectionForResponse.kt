package com.example.parental1.model

// Response for /verify-connection endpoint
data class ConnectionForResponse(
    val success: Boolean,
    val message: String
)