package com.example.parental1.model

// Request for /verify-connection endpoint
data class ConnectionForRequest(
    val parent_email: String,
    val child_email: String
)