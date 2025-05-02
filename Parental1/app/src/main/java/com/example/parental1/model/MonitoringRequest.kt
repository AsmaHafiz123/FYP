package com.example.parental1.model

data class MonitoringRequest(
    val ParentEmail: String,
    val childEmail: String,
    val imei: String,
    val requestStatus: String = "pending" // Default value set to "pending"
)




