package com.example.parental1.network

import com.example.parental1.model.EmailResponse
import com.example.parental1.model.SignupRequest
import com.example.parental1.model.SignupResponse
import com.example.parental1.model.LoginRequest
import com.example.parental1.model.LoginResponse
import com.example.parental1.model.EmailRequest
import com.example.parental1.model.UpdatePasswordResponse
import com.example.parental1.model.UpdatePasswordRequest
import com.example.parental1.model.RoleSelection
import com.example.parental1.model.RoleResponse
import com.example.parental1.model.VerifyCodeRequest
import com.example.parental1.model.VerifyCodeResponse
import com.example.parental1.model.CheckEmailResponse
import com.example.parental1.model.ConnectionForRequest
import com.example.parental1.model.ConnectionForResponse
import com.example.parental1.model.QRCodeResponse
import com.example.parental1.model.DeviceConnectionRequest
import com.example.parental1.model.DeviceConnectionResponse
import com.example.parental1.model.ScreenDataResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.Response
import retrofit2.http.Header

interface ApiService {

    //Send screenshot
    @POST("/screen-data")
    fun sendScreenData(@Body screenData: Map<String, String>): Call<Unit>


    // Verify connection between parent and child
    @POST("verify-connection")
    fun verifyConnection(
        @Header("Authorization") token: String,
        @Body request: ConnectionForRequest
    ): Call<ConnectionForResponse>

    // Check if parent email exists for QR code generation
    @GET("ParentEmailQrCode")
    fun checkEmailForQrCode(@Query("email") email: String): Call<CheckEmailResponse>

    // Generate QR code for parent and child emails
    @GET("generateQRCode")
    fun generateQRCode(@Query("emails") emails: String): Call<QRCodeResponse>

    // Add or update device connection
    @POST("add-device-connection")
    fun addDeviceConnection(@Body request: DeviceConnectionRequest): Call<DeviceConnectionResponse>

    //All necessary signup to update password
    @POST("forgot-password")
    fun forgotPassword(@Body request: EmailRequest): Call<EmailResponse>

    @POST("verify-reset-code")
    fun verifyResetCode(@Body request: VerifyCodeRequest): Call<VerifyCodeResponse>

    @POST("update-password")
    fun updatePassword(@Body request: UpdatePasswordRequest): Call<UpdatePasswordResponse>

    // Endpoint to trigger the sending of the verification email
    @GET("/send-verification-email")
    fun sendVerificationEmail(): Call<Void>

    @POST("/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // Corrected signup method to match backend route
    @POST("/signup")
    fun signup(@Body signupRequest: SignupRequest): Call<SignupResponse>

    @GET("api/check-server")  // Adjusted to use "/api"
    fun checkServer(): Call<String>

    @POST("api/data") // Backend API endpoint
    fun sendDataToBackend(@Body requestData: Map<String, Any>): Call<SignupResponse>

    @POST("/selectRole")
    fun selectRole(@Body role: RoleSelection): Call<RoleResponse>

    // Define the check email methods
    @GET("parent/checkEmail")
    fun checkEmailInParent(@Query("email") email: String): Call<Boolean>

    @GET("child/checkEmail")
    fun checkEmailInChild(@Query("email") email: String): Call<Boolean>

    // Refactored function to check email in both parent and child tables
    fun checkEmailExists(email: String, callback: (Boolean) -> Unit) {
        val parentCall = checkEmailInParent(email)
        val childCall = checkEmailInChild(email)

        // Perform both checks simultaneously
        parentCall.enqueue(object : retrofit2.Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful && response.body() == true) {
                    callback(true) // Email exists in the parent table
                } else {
                    // Proceed to check the child table if the parent table check fails
                    childCall.enqueue(object : retrofit2.Callback<Boolean> {
                        override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                            if (response.isSuccessful && response.body() == true) {
                                callback(true) // Email exists in the child table
                            } else {
                                callback(false) // Email not found in either table
                            }
                        }

                        override fun onFailure(call: Call<Boolean>, t: Throwable) {
                            callback(false) // Handle failure (e.g., network error)
                        }
                    })
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                // Handle failure (e.g., network error)
                callback(false)
            }
        })
    }


}