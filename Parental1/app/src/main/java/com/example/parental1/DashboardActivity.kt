package com.example.parental1 // Replace with your actual package name

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView



class DashboardActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard) // Link to your XML layout

        // Initialize TextView for Dashboard Title (Optional - if you need to manipulate it programmatically)
        val dashboardTitle: TextView = findViewById(R.id.dashboard_title)

        // You can modify the title here if needed
        dashboardTitle.text = "Parent_Dashboard"
    }
}
