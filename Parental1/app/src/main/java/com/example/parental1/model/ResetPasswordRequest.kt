//

package com.example.parental1.model

data class ResetPasswordRequest(
    val code: String,
    val email: String // Ensure this field exists
)
