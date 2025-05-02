package com.example.parental1.model

data class UpdatePasswordResponse(
    val success: Boolean,    // Indicates if the update was successful
    val message: String      // Message returned by the server (e.g., "Password updated successfully")
)
