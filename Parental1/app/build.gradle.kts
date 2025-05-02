plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)


}

android {
    namespace = "com.example.parental1"
    compileSdk = 35 // latest SDK version

    defaultConfig {
        applicationId = "com.example.parental1"
        minSdk = 23
        //noinspection EditedTargetSdkVersion,OldTargetApi
        targetSdk = 35 // latest SDK version
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0" // Ensure this matches your Compose version
    }
}

dependencies {
        // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)  // Ensure appcompat is included
    implementation(libs.material)
    implementation(libs.material3)
    implementation(libs.androidx.ui.tooling.preview.v121)
    implementation(libs.androidx.ui.v121)
    implementation(libs.androidx.ui.tooling.v121)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    implementation (libs.androidx.appcompat.appcompat.v161)
    implementation (libs.material.v190)
    //noinspection UseTomlInstead
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation(libs.kotlinx.coroutines.android)
    implementation (libs.appcompat.v7) // Check if this is the latest version
    implementation (libs.firebase.auth) // Update to the latest version")

    // Vector drawable updates
    implementation (libs.androidx.vectordrawable)
    implementation (libs.androidx.vectordrawable.animated)






    // Compose dependencies
    implementation(libs.ui)
    implementation(libs.androidx.material)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3.android)
    testImplementation(libs.junit.junit)
    testImplementation(libs.junit.junit)


    // Optional: Jetpack Compose UI Test dependencies
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)




    // Coroutine support for Retrofit (for asynchronous tasks)
    implementation (libs.kotlinx.coroutines.android.v170)

    // For connectivity
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.kotlinx.coroutines.android)
    implementation (libs.androidx.appcompat) // or the latest version
    implementation (libs.socket.io.client)
    implementation (libs.kotlinx.coroutines.core)
    implementation (libs.kotlinx.coroutines.android)
    implementation (libs.logging.interceptor.v4100)
    implementation (libs.zxing.core)
    implementation (libs.zxing.android)
    implementation (libs.okhttp)

}




