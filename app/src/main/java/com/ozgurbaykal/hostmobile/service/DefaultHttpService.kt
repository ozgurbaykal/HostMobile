package com.ozgurbaykal.hostmobile.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.google.gson.Gson
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.CustomServerController
import com.ozgurbaykal.hostmobile.control.SharedPreferenceManager
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Date

class DefaultHttpService : Service() {

    private val TAG = "_DefaultHttpService"

    val jwtSecret = "NqDU0Bgcx7SEaJPZkNNYxTbsmJl5u"  // Bu anahtarı güvenli bir yerde saklamalısınız.
    val jwtIssuer = "ozgurbaykal"
    val jwtRealm = "hostmobile"

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        Log.i(TAG, "onBind() -> ")
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
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

    fun makeToken(): String {
        val jwtAlgorithm = Algorithm.HMAC256(jwtSecret)
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withClaim("name", "ozgurbaykal")  // Burada "YourUsernameHere" yerine istediğiniz değeri koyabilirsiniz.
            .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))  // 1 saatlik süre.
            .sign(jwtAlgorithm)
    }

    fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }


    private val server = embeddedServer(Netty, port = 49761,"0.0.0.0") {

        intercept(ApplicationCallPipeline.Plugins) {
            Log.i(TAG, "Intercept block started")

        }



        install(ContentNegotiation) {
            json()
            gson()
        }

        install(Authentication) {
            Log.i(TAG, "install Authentication BLOĞUNDA")

            jwt {
                Log.i(TAG, "JWT BLOĞUNDA")
                realm = jwtRealm
                verifier(
                    JWT.require(Algorithm.HMAC256(jwtSecret))
                        .withIssuer(jwtIssuer)
                        .build())
                validate { credential ->
                    val jwt = credential.payload

                    val name = jwt.getClaim("name").asString()
                    Log.i(TAG, "JWT validate function called name: $name")
                    if (name != null) UserIdPrincipal(name) else null
                }
            }
        }

        routing {

            get("/{...}") {
                var requestPath = call.request.path().substring(1)

                    val cookieToken = call.request.cookies["auth_token"]

                    //EĞER TOKEN NULL İSE (YANİ İLK GİRİŞ V.S.) GİRİŞ SAYFASINI GÖSTER
                    if (cookieToken == null) {


                        val assetManager = applicationContext.assets
                        Log.i(TAG, "İLK REQUESTPATH: " + requestPath)
                        if (requestPath.isEmpty()) {
                            Log.i(TAG, "auth-login-default.html için if bloğuna girdi")
                            val authContent = assetManager.open("auth-login-default.html").use { it.readBytes() }
                            call.respondBytes(authContent, ContentType.Text.Html)
                            return@get
                        }else{
                            try {
                                // assets klasöründe bu dosya var mı kontrol et
                                requestPath.split("-").last()
                                Log.i(TAG, "auth-login-default.html için ELSE bloğuna girdi requestPath: " + requestPath)

                                val contentType = getContentType(File(requestPath)) // İçerik türünü al

                                // Dosyanın içeriğini doğrudan bayt olarak oku
                                val fileContent = assetManager.open(requestPath).use { it.readBytes() }

                                // İçeriği yanıt olarak gönder
                                call.respondBytes(fileContent, contentType)

                                return@get
                            } catch (e: IOException) {
                                Log.e(TAG, "DOSYA BULAMADI")
                                // Dosya bulunamadıysa bu bloğa girecektir.
                            }
                        }


                    }

                    try {
                        JWT.require(Algorithm.HMAC256(jwtSecret))
                            .withIssuer(jwtIssuer)
                            .build()
                            .verify(cookieToken)
//TOKEN DOĞRULAMASI YUKARIDA BAŞARILI OLURSA DEFAULT SERVER İLE İLGİLİ SİTEYİ GÖSTER (API)

                            Log.i(TAG, "DEFAULT SERVER JWT TOKENİ DOĞRU")

                    } catch (e: JWTVerificationException) {

                        //TOKEN DOĞRULAMASI BAŞARILI OLMADIĞI TAKDİRDE, GİRİŞ SAYFASI GÖSTER
                        val assetManager = applicationContext.assets
                        Log.i(TAG, "İLK REQUESTPATH: " + requestPath)
                        if (requestPath.isEmpty()) {
                            Log.i(TAG, "auth-login-default.html için if bloğuna girdi")
                            val authContent = assetManager.open("auth-login-default.html").use { it.readBytes() }
                            call.respondBytes(authContent, ContentType.Text.Html)
                            return@get
                        }else{
                            try {
                                // assets klasöründe bu dosya var mı kontrol et
                                requestPath.split("-").last()
                                Log.i(TAG, "auth-login-default.html için ELSE bloğuna girdi requestPath: " + requestPath)

                                val contentType = getContentType(File(requestPath)) // İçerik türünü al

                                // Dosyanın içeriğini doğrudan bayt olarak oku
                                val fileContent = assetManager.open(requestPath).use { it.readBytes() }

                                // İçeriği yanıt olarak gönder
                                call.respondBytes(fileContent, contentType)

                                return@get
                            } catch (e: IOException) {
                                Log.e(TAG, "DOSYA BULAMADI")
                                // Dosya bulunamadıysa bu bloğa girecektir.
                            }
                        }


                    }
            }




            //EN SON BURAYI CHATGPTDEN ALDIĞIMI EKLEMEDİM BURAYI EKLEYİNCE Bİ HATA OLUYODU BAKCAKTIM GERİ ALDIM
            post("/postAuthPassword") {
                try {
                    val requestData = call.receiveText()
                    Log.i(TAG, "Received data in /postAuthPassword -> : $requestData")

                    // Gson ile JSON'dan haritaya dönüşüm yap
                    val data: Map<String, String> = Gson().fromJson(requestData, Map::class.java) as Map<String, String>
                    val clientPassword = data["password"]

                    val serverPasswordEncrypted = sha256(DefaultServerData.defaultServerAuthPassword.toString())  // sunucudaki şifreyi şifreleyin

                    if (clientPassword == serverPasswordEncrypted) {
                        // Şifre doğru
                        Log.i(TAG, "Şifre Doğru")
                        val token = makeToken()
                        call.response.cookies.append(Cookie("auth_token", token, path = "/", httpOnly = true, maxAge = 3600)) // 1 saat
                        call.respond(ResponseDto(true, token))
                    } else {
                        // Şifre yanlış
                        Log.i(TAG, "Şifre yanlış")
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
        Log.i(TAG, "onStartCommand() -> ")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        server.stop(1000, 5000)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2)

        Log.i(TAG, "onDestroy() ->")
        super.onDestroy()
    }

    private fun startForegroundService() {
        Log.i(TAG, " startForegroundService() -> ")
        val notification = NotificationCompat.Builder(this, "CHANNEL_SERVICE")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.custom_edit_server_icon)
            .setContentTitle("Default Server Running!")
            .setContentText("Your default mobile server service is active and running on " + 49761 + " port")
            .build()

        startForeground(2, notification)
    }

}