package com.ozgurbaykal.hostmobile.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.CustomServerController
import com.ozgurbaykal.hostmobile.model.AppDatabase
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File

class CustomHttpService : Service() {

    private val TAG = "_CustomHttpService"
    private lateinit var db: AppDatabase

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        Log.i(TAG, "onBind() -> ")
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {

      db = AppDatabase.getDatabase(this)

        startForegroundService()
        super.onCreate()
    }

    private fun getContentType(file: File): ContentType {
        return when (file.extension) {
            "html" -> ContentType.Text.Html
            "css" -> ContentType.Text.CSS
            "js" -> ContentType.Application.JavaScript
            "jpg", "jpeg" -> ContentType.Image.JPEG
            "png" -> ContentType.Image.PNG
            else -> ContentType.Application.OctetStream
        }
    }


    private val server = embeddedServer(Netty, port = CustomServerController.customServerPort,"0.0.0.0") {

        intercept(ApplicationCallPipeline.Monitoring) {
            val request = call.request
            Log.i(TAG, "Received request: ${request.httpMethod} ${request.uri}")
        }



        routing {
            get("/{...}") {
                val dao = db.folderDao()
                val folder = dao.getSelectedFolder()
                val folderPath = "${getExternalFilesDir(null)?.path}/${folder?.folderName}"

                // İstek yolu, istemcinin istediği dosyanın yolu olacak.
                var requestPath = call.request.path().substring(1)

                // Dosyanın tam yolu, seçili klasör yolu ile istek yolu birleştirilerek oluşturulur.
                var fullPath = folderPath + File.separator + requestPath

                if(requestPath.isEmpty())
                    fullPath = folder?.selectedFilePath.toString()

                Log.i(TAG,  "folderPath: " + folderPath + "  File.separator: " + File.separator + "  requestPath: " + requestPath)
                val file = File(fullPath)
                if (file.exists()) {
                    Log.i(TAG, "file is exist FILE PATH: " + fullPath)
                    val contentType = getContentType(file)
                    val content = file.readBytes()
                    call.respondBytes(content, contentType)
                } else {
                    Log.i(TAG, "file doesn't exist FILE PATH: " + fullPath)
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        Log.i(TAG, "server() ->")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        server.start(wait = false)
        Log.i(TAG, "onStartCommand() -> PORT: " + CustomServerController.customServerPort)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        server.stop(1000, 5000)
        Log.i(TAG, "onDestroy() ->")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)

        super.onDestroy()
    }

    private fun startForegroundService() {
        Log.i(TAG, " startForegroundService() -> ")
        val notification = NotificationCompat.Builder(this, "CHANNEL_SERVICE")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.custom_edit_server_icon)
            .setContentTitle("Custom Server Running!")
            .setContentText("Your custom mobile server service is active and running on " + CustomServerController.customServerPort + " port")
            .build()

        startForeground(1, notification)
    }

}