package com.ozgurbaykal.hostmobile.service

import java.net.ServerSocket

object NetworkUtils {

    fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (ex: Exception) {
            false
        }
    }
    
}