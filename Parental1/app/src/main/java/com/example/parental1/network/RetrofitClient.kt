//Api client, child main, multi feature job service,

package com.example.parental1.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Replace with your actual backend URL
    private const val BASE_URL = "http://192.168.84.46:3000/" // Ensure the URL ends with a slash

    // Using lazy initialization for the ApiService
    val apiService: ApiService by lazy {
        // Logging interceptor for debugging network calls
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // OkHttpClient with logging and timeouts
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Add the logging interceptor
            .connectTimeout(60, TimeUnit.SECONDS) // Timeout for connecting
            .writeTimeout(60, TimeUnit.SECONDS)   // Timeout for writing
            .readTimeout(60, TimeUnit.SECONDS)    // Timeout for reading
            .build()

        // Retrofit instance
        Retrofit.Builder()
            .baseUrl(BASE_URL)  // Set the base URL of your API
            .addConverterFactory(GsonConverterFactory.create())  // Use Gson for JSON conversion
            .client(client) // Add OkHttpClient
            .build()
            .create(ApiService::class.java) // Create an instance of the ApiService
    }
}




















/*package com.example.parental1.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit



object RetrofitClient {

    // Replace with your actual backend URL

//ipconfig: for checking ip if you get network error.my
    private const val BASE_URL = "http://192.168.66.46:3000"
    // Ensure the URL has a trailing slash

    // Using lazy initialization for the ApiService
    val apiService: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)  // Set the base URL of your API
            .addConverterFactory(GsonConverterFactory.create())  // Use Gson for JSON conversion
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)  // Timeout for connecting
                    .writeTimeout(30, TimeUnit.SECONDS)    // Timeout for writing
                    .readTimeout(30, TimeUnit.SECONDS)     // Timeout for reading
                    .build()
            )
            .build()

        retrofit.create(ApiService::class.java)  // Create an instance of the ApiService
    }
}
*/