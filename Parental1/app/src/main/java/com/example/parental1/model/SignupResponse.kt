// For signup: http://localhost:3000/signup

package com.example.parental1.model

data class SignupResponse(
    val success: Boolean,
    val message: String,
    val token: String?, // Add token field
    val user: User? // Add user field
)


//data class SignupResponse(
  //  val success: Boolean,
    //val message: String
//)
