package com.example.parental1 // Make sure this matches your actual package name

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home) // This connects to the layout file activity_home.xml
    }
}
