// Explanation: This data class represents the data you’ll send when logging in.
// You can modify the fields according to your backend requirements.
// For login: http://localhost:3000/login
// For real device: http://192.168.176.46:3000/login

package com.example.parental1.model

data class LoginRequest(
    val email: String,
    val password: String,
    val role: String
)
