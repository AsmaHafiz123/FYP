package com.example.parental1.network

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {

    private var mSocket: Socket? = null

    // Initialize the socket connection
    fun initializeSocket() {
        try {
            if (mSocket == null) {
                // Replace with your server URL
                mSocket = IO.socket("http://192.168.66.46:3000") // Your backend socket URL
                mSocket?.connect()

                // Add event listeners here
                mSocket?.on(Socket.EVENT_CONNECT) {
                    // Handle successful connection
                    println("Socket connected")
                }

                mSocket?.on("sendNotification") { args ->
                    // Handle the 'sendNotification' event from the server
                    val message = args[0] as String
                    println("Received message: $message")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Listen to events from the server
    fun onEvent(event: String, listener: (Array<Any>) -> Unit) {
        mSocket?.on(event) { args ->
            listener(args)
        }
    }

    // Send a message to the server
    fun sendMessageToServer(message: String) {
        try {
            // Create a JSON object with message details (or any other data structure you need)
            val jsonObject = JSONObject()
            jsonObject.put("message", message)

            // Emit the message to the server using socket
            mSocket?.emit("sendNotification", jsonObject)  // Replace "sendNotification" with your server's event name
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Disconnect the socket when done
    fun disconnectSocket() {
        mSocket?.disconnect()
    }
}
