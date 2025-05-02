package com.example.parental1.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient

object ApiClient {
    private const val BASE_URL = "http://192.168.84.46:3000/" // Replace with the actual API base URL

    private val client = OkHttpClient.Builder().build()

    // This property provides the Retrofit instance
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // This is the method that returns the Retrofit client instance
    fun getClient(): Retrofit {
        return retrofit
    }
}






