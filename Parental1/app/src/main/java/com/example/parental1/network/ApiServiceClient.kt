package com.example.parental1.network

import retrofit2.Retrofit

object ApiServiceClient {
    // Using Retrofit instance from ApiClient to create ApiService
    val apiService: ApiService = ApiClient.retrofit.create(ApiService::class.java)
}
