package com.example.parental1.model


// ResetCodeResponse data class
data class EmailResponse(
    val message: String,
    val resetCode: Int // Only for testing, don't send this in production
)