package com.ozgurbaykal.hostmobile.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.google.gson.Gson
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.CustomServerController
import com.ozgurbaykal.hostmobile.control.SharedPreferenceManager
import com.ozgurbaykal.hostmobile.model.AppDatabase
import com.ozgurbaykal.hostmobile.view.DefaultServerFragment
import com.ozgurbaykal.hostmobile.view.LogFragment
import com.ozgurbaykal.hostmobile.view.MainActivity
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.serialization.gson.gson
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing.Plugin.install
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8
import io.ktor.server.application.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.Authentication
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.Date

class CustomHttpService : Service() {

    private val TAG = "_CustomHttpService"
    private lateinit var db: AppDatabase

    val jwtSecret = "NqDU0Bgcx7SEaJPZkNNYxTbsmJl5u"  // Bu anahtarı güvenli bir yerde saklamalısınız.
    val jwtIssuer = "ozgurbaykal"
    val jwtRealm = "hostmobile"

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
    fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }


    fun makeToken(): String {
        val jwtAlgorithm = Algorithm.HMAC256(jwtSecret)
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withClaim("name", "ozgurbaykal")  // Burada "YourUsernameHere" yerine istediğiniz değeri koyabilirsiniz.
            .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))  // 1 saatlik süre.
            .sign(jwtAlgorithm)
    }



    private val server = embeddedServer(Netty, port = CustomServerController.customServerPort,"0.0.0.0") {

        intercept(ApplicationCallPipeline.Plugins) {
            Log.i(TAG, "Intercept block started")

        }



        install(ContentNegotiation) {
            json()
            gson()
        }

        install(Authentication) {
            jwt("auth-jwt") {
                realm = jwtRealm
                val jwtAlgorithm = Algorithm.HMAC256(jwtSecret)
                verifier(JWT.require(jwtAlgorithm).withIssuer(jwtIssuer).build())

                authHeader { call ->
                    val cookieValue = call.request.cookies["auth_token"] ?: return@authHeader null
                    Log.i(TAG, " cookieValue: " + cookieValue)

                    try {
                        parseAuthorizationHeader("Bearer $cookieValue")
                    } catch (cause: IllegalArgumentException) {
                        cause.message
                        null
                    }
                }

                validate { credential ->
                    if (credential.payload.getClaim("name").asString() == jwtIssuer) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }





        routing {

            get("/{...}") {
                var requestPath = call.request.path().substring(1)

                //EĞER UYGULAMADAKİ OPEN AUTH SEÇENEĞİ AÇIKSA BU BLOĞA GİR VE TOKEN DOĞRULAMALARI YAP DEĞİLSE ELSE BLOĞUNA GEÇ
                if (SharedPreferenceManager.readBoolean("customServerAuthBoolean", false) == true ) {

                    val cookieToken = call.request.cookies["auth_token"]

                    //EĞER TOKEN NULL İSE (YANİ İLK GİRİŞ V.S.) GİRİŞ SAYFASINI GÖSTER
                    if (cookieToken == null) {
                        MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()}   JWT token is null in the request to the server. Redirect to home page. ", ContextCompat.getColor(applicationContext, R.color.custom_red), false)
                        MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()}   Request ->  $requestPath", Color.WHITE, false)

                        val fileExtension = requestPath.substringAfterLast('.', "").toLowerCase()
                        Log.i(TAG, "fileExtension:  $fileExtension")


                        if (fileExtension == "html" || fileExtension.isEmpty()) {
                            // COKKIE NULL OLDUĞU İÇİN GİRİŞ SAYFASINA YÖNLENDİR
                            val authContent = applicationContext.assets.open("auth-login.html")
                                .use { it.readBytes() }
                            call.respondBytes(authContent, ContentType.Text.Html)
                            return@get
                        } else {
                            // JS, CSS, resim vb. için
                            try {
                                requestPath = requestPath.split("/").last()
                                Log.i(TAG, "COOKIE NULL REQUESTPATH: " + requestPath)

                                val contentType = getContentType(File(requestPath))
                                val fileContent = applicationContext.assets.open(requestPath).use { it.readBytes() }
                                call.respondBytes(fileContent, contentType)
                                return@get
                            } catch (e: IOException) {
                                // Dosya bulunamazsa hata logu
                                Log.e(TAG, "COOKIE NULL BLOĞUNDA DOSYA BULAMADI")
                            }
                        }

                    }

                    try {
                        JWT.require(Algorithm.HMAC256(jwtSecret))
                            .withIssuer(jwtIssuer)
                            .build()
                            .verify(cookieToken)
                        //TOKEN DOĞRULAMASI YUKARIDA BAŞARILI OLURSA KULLANICININ SEÇTİĞİ WEB SİTESİNİ ARTIK GÖSTER
                        MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()}   JWT token is true.", ContextCompat.getColor(applicationContext, R.color.custom_green), false)
                        MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()}   Request ->  $requestPath", Color.WHITE, false)

                        val dao = db.folderDao()
                        val folder = dao.getSelectedFolder()
                        val folderPath = "${getExternalFilesDir(null)?.path}/${folder?.folderName}"


                        // İstek yolu, istemcinin istediği dosyanın yolu olacak.

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
                            val token = makeToken()
                            call.response.cookies.append(Cookie("auth_token", token, path = "/", httpOnly = true, maxAge = 3600)) // 1 saat
                            call.respondBytes(content, contentType)
                        } else {
                            Log.i(TAG, "file doesn't exist FILE PATH: " + fullPath)
                            call.respond(HttpStatusCode.NotFound)
                        }

                    } catch (e: JWTVerificationException) {

                        MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()}   Can't verify JWT token, redirect to home page.", ContextCompat.getColor(applicationContext, R.color.custom_red), false)
                        MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()}   Request ->  $requestPath", Color.WHITE, false)

                        //TOKEN DOĞRULAMASI BAŞARILI OLMADIĞI TAKDİRDE, GİRİŞ SAYFASI GÖSTER
                        val fileExtension = requestPath.substringAfterLast('.', "").toLowerCase()
                        Log.i(TAG, "fileExtension:  $fileExtension")

                        if (fileExtension == "html" || fileExtension.isEmpty()) {
                            // COKKIE NULL OLDUĞU İÇİN GİRİŞ SAYFASINA YÖNLENDİR
                            val authContent = applicationContext.assets.open("auth-login.html")
                                .use { it.readBytes() }
                            call.respondBytes(authContent, ContentType.Text.Html)
                            return@get
                        } else {
                            // JS, CSS, resim vb. için
                            try {
                                requestPath = requestPath.split("/").last()
                                Log.i(TAG, "COOKIE NULL REQUESTPATH: " + requestPath)

                                val contentType = getContentType(File(requestPath))
                                val fileContent = applicationContext.assets.open(requestPath).use { it.readBytes() }
                                call.respondBytes(fileContent, contentType)
                                return@get
                            } catch (e: IOException) {
                                // Dosya bulunamazsa hata logu
                                Log.e(TAG, "COOKIE NULL BLOĞUNDA DOSYA BULAMADI")
                            }
                        }


                    }



                }else{
                    val dao = db.folderDao()
                    val folder = dao.getSelectedFolder()
                    val folderPath = "${getExternalFilesDir(null)?.path}/${folder?.folderName}"


                    // İstek yolu, istemcinin istediği dosyanın yolu olacak.

                    // Dosyanın tam yolu, seçili klasör yolu ile istek yolu birleştirilerek oluşturulur.
                    var fullPath = folderPath + File.separator + requestPath

                    MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()}   Request ->  $requestPath", Color.WHITE, false)

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




            //EN SON BURAYI CHATGPTDEN ALDIĞIMI EKLEMEDİM BURAYI EKLEYİNCE Bİ HATA OLUYODU BAKCAKTIM GERİ ALDIM
            post("/postAuthPassword") {
                try {
                    val requestData = call.receiveText()
                    Log.i(TAG, "Received data in /postAuthPassword -> : $requestData")

                    MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()}   POST AUTH PASSWORD REQUEST", Color.WHITE, false)

                    // Gson ile JSON'dan haritaya dönüşüm yap
                    val data: Map<String, String> = Gson().fromJson(requestData, Map::class.java) as Map<String, String>
                    val clientPassword = data["password"]

                    val serverPasswordEncrypted = sha256(CustomServerData.customServerAuthPassword.toString())  // sunucudaki şifreyi şifreleyin

                    if (clientPassword == serverPasswordEncrypted) {
                        // Şifre doğru
                        Log.i(TAG, "Şifre Doğru")
                        MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()}   /postAuthPassword -> CORRECT PASSWORD", ContextCompat.getColor(applicationContext, R.color.custom_green), false)
                        val token = makeToken()
                        call.response.cookies.append(Cookie("auth_token", token, path = "/", httpOnly = true, maxAge = 3600)) // 1 saat
                        call.respond(ResponseDto(true, token))
                    } else {
                        // Şifre yanlış
                        Log.i(TAG, "Şifre yanlış")
                        MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()}   /postAuthPassword -> INCORRECT PASSWORD", ContextCompat.getColor(applicationContext, R.color.custom_red), false)

                        call.respond(ResponseDto(false))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "postAuthPassword HATA : ", e)
                    call.respond(HttpStatusCode.Forbidden, mapOf("password_status" to "Failed to process request"))
                }
            }
        }

        Log.i(TAG, "server() ->")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        server.start(wait = false)
        Log.i(TAG, "onStartCommand() -> PORT: " + CustomServerController.customServerPort)
        MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()} CustomServer is started.", ContextCompat.getColor(applicationContext, R.color.custom_green), false)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        server.stop(1000, 5000)
        Log.i(TAG, "onDestroy() ->")
        MainActivity.getInstance()?.addLogFromInstance("CS ${MainActivity.getInstance()?.getCurrentTime()} CustomServer is stopped.", ContextCompat.getColor(applicationContext, R.color.custom_red), false)

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