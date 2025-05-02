package com.example.parental1.model

data class ScreenDataResponse(
    val success: Boolean,
    val message: String,
    val insertId: Long? = null
)