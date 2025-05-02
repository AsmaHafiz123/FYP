package com.example.parental1.model

data class MonitoringRequestResponse(
    val hasRequest: Boolean,        // Indicates if a request exists
    val parentEmail: String?,       // Parent's email
    val requestId: Int?             // ID of the monitoring request
)
