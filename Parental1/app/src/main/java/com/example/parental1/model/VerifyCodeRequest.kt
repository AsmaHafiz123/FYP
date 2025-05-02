package com.example.parental1.model

data class VerifyCodeRequest(
    val email: String,
    val code: String
)