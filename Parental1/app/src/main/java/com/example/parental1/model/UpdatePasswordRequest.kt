package com.example.parental1.model

data class UpdatePasswordRequest(
    val email: String,       // User's email address for identification
    val newPassword: String , // New password entered by the user
    val resetCode: String
)
