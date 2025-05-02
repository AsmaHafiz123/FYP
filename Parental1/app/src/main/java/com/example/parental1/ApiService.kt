package com.example.parental1



import com.example.parental1.model.EmailRequest
import com.example.parental1.model.EmailResponse
import com.example.parental1.model.LoginRequest
import com.example.parental1.model.LoginResponse
import com.example.parental1.model.ResetPasswordRequest
import com.example.parental1.model.ResetPasswordResponse
import com.example.parental1.model.SignupRequest
import com.example.parental1.model.SignupResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("/verify-reset-code")  // The correct endpoint for your verification API
    fun verifyResetCode(@Body resetPasswordRequest: ResetPasswordRequest): Call<ResetPasswordResponse>

    // Add other API methods here

    // Endpoint to trigger the sending of the verification email
    @GET("/send-verification-email")
    fun sendVerificationEmail(): Call<Void>



    @POST("/forgot-password")
    fun forgotPassword(@Body forgotPasswordRequest: EmailRequest): Call<EmailResponse>

    @POST("/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>


    // Corrected signup method to match backend route
    @POST("/signup")  // Make sure this is /signup"
    fun signup(@Body signupRequest: SignupRequest): Call<SignupResponse>


    @GET("api/check-server")  // Adjusted to use "/api"
    fun checkServer(): Call<String>

    @POST("api/data") // Backend API endpoint
    fun sendDataToBackend(@Body requestData: Map<String, Any>): Call<SignupResponse>

}


