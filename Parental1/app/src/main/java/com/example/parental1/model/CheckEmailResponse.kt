package com.example.parental1.model

// Response for /ParentEmailQrCode endpoint
data class CheckEmailResponse(
    val success: Boolean,
    val exists: Boolean
)