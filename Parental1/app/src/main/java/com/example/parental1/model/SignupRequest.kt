//Explanation: This data class represents the data you’ll send when signing up.
// You can add or modify the fields according to the data required by your backend for
// signup (e.g., username, email, password, etc.).
// For signup: http://localhost:3000/signup
// For real device: http://192.168.176.46:3000/signup
package com.example.parental1.model

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String // Ensure the role is included here
)

