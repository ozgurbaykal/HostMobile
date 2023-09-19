package com.ozgurbaykal.hostmobile.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Environment
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
import com.ozgurbaykal.hostmobile.model.CustomServerFolders
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.readAllParts
import io.ktor.http.content.streamProvider
import io.ktor.http.headers
import io.ktor.serialization.gson.gson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.path
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveOrNull
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.netty.handler.codec.DefaultHeaders
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Date
import com.auth0.jwt.interfaces.JWTVerifier
import com.ozgurbaykal.hostmobile.view.MainActivity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DefaultHttpService : Service() {

    private val TAG = "_DefaultHttpService"

    val jwtSecret =
        "NqDU0Bgcx7SEaJPZkNNYxTbsmJl5u"  // Bu anahtarı güvenli bir yerde saklamalısınız.
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
            .withClaim(
                "name",
                "ozgurbaykal"
            )
            .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))  // 1 saatlik süre.
            .sign(jwtAlgorithm)
    }
    fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }


    private val server = embeddedServer(Netty, port = 49761, "0.0.0.0") {

        intercept(ApplicationCallPipeline.Plugins) {
            Log.i(TAG, "Intercept block started")
        }



        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                ignoreUnknownKeys = true
                allowSpecialFloatingPointValues = true
                useArrayPolymorphism = true
            })
            gson()
        }


        install(CORS) {
            anyHost()
            methods += HttpMethod.Options
            methods += HttpMethod.Post
            methods += HttpMethod.Get
            headers += HttpHeaders.ContentType
            headers += HttpHeaders.AccessControlAllowOrigin
            headers += HttpHeaders.AccessControlAllowMethods
            headers += HttpHeaders.AccessControlAllowHeaders
            headers += HttpHeaders.AccessControlAllowCredentials


            allowCredentials = true
            maxAgeInSeconds = 24 * 60 * 60
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

                val cookieToken = call.request.cookies["auth_token"]

                //EĞER TOKEN NULL İSE (YANİ İLK GİRİŞ V.S.) GİRİŞ SAYFASINI GÖSTER
                if (cookieToken == null) {
                    MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   JWT token is null in the request to the server. Redirect to home page. ", ContextCompat.getColor(applicationContext, R.color.custom_red), false)
                    MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   Request ->  $requestPath", Color.WHITE, false)

                    val fileExtension = requestPath.substringAfterLast('.', "").toLowerCase()
                    Log.i(TAG, "fileExtension:  $fileExtension")

                    if (fileExtension == "html" || fileExtension.isEmpty()) {
                        // COKKIE NULL OLDUĞU İÇİN GİRİŞ SAYFASINA YÖNLENDİR
                        val authContent = applicationContext.assets.open("auth-login-default.html")
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
                    //TOKEN DOĞRULAMASI YUKARIDA BAŞARILI OLURSA DEFAULT SERVER ANA SAYFASINI GÖSTER (API)
                    Log.i(TAG, "DEFAULT SERVER JWT TOKENİ DOĞRU")

                    MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   JWT token is true.", ContextCompat.getColor(applicationContext, R.color.custom_green), false)
                    MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   Request ->  $requestPath", Color.WHITE, false)

                    val assetManager = applicationContext.assets

                    if (requestPath.isEmpty()) {

                        Log.i(TAG, "TOKEN ARTIK DOĞRU, GİRİŞ SAYFASI YERİNE ANA SAYFAYI GÖSTER")
                        val authContent =
                            assetManager.open("DefaultServerWeb/default-server-main.html")
                                .use { it.readBytes() }
                        val token = makeToken()
                        call.response.cookies.append(Cookie("auth_token", token, path = "/", httpOnly = true, maxAge = 3600)) // 1 saat
                        call.respondBytes(authContent, ContentType.Text.Html)
                        return@get
                    } else {
                        try {

                            //BU SERVİSTE TÜM DOSYALAR DefaultServerWeb KLASÖRÜ İÇERİSİNDE OLACAĞI İÇİN KLASÖR İSMİNİ BAŞA EKLİYORUZ
                            requestPath = "DefaultServerWeb/" + requestPath
                            Log.i(TAG, "default-server-main.html için ELSE bloğuna girdi requestPath: " + requestPath)

                            val contentType = getContentType(File(requestPath))

                            val fileContent = assetManager.open(requestPath).use { it.readBytes() }
                            val token = makeToken()
                            call.response.cookies.append(Cookie("auth_token", token, path = "/", httpOnly = true, maxAge = 3600)) // 1 saat
                            call.respondBytes(fileContent, contentType)
                            return@get
                        } catch (e: IOException) {
                            Log.e(TAG, "DOSYA BULAMADI")
                            // Dosya bulunamadıysa bu bloğa girecektir.
                        }
                    }


                } catch (e: JWTVerificationException) {

                    MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   Can't verify JWT token, redirect to home page.", ContextCompat.getColor(applicationContext, R.color.custom_red), false)
                    MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   Request ->  $requestPath", Color.WHITE, false)

                    //TOKEN DOĞRULAMASI BAŞARILI OLMADIĞI TAKDİRDE, GİRİŞ SAYFASI GÖSTER
                    Log.e(TAG, "TOKEN VERİFİCATİON ERROR: ")
                    e.message
                    val fileExtension = requestPath.substringAfterLast('.', "").toLowerCase()
                    Log.i(TAG, "fileExtension:  $fileExtension")

                    if (fileExtension == "html" || fileExtension.isEmpty()) {
                        // COKKIE NULL OLDUĞU İÇİN GİRİŞ SAYFASINA YÖNLENDİR
                        val authContent = applicationContext.assets.open("auth-login-default.html")
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
            }


            //ROUTE CODE 02
            authenticate("auth-jwt") {
                post("/postWebFolders") {
                    MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   POST WEB FOLDERS REQUEST", Color.WHITE, false)

                    try {
                        val multipart = call.receiveMultipart()
                        var folderName: String? = null
                        val appSpecificExternalDir =
                            ContextCompat.getExternalFilesDirs(applicationContext, null)[0]

                        val parts = multipart.readAllParts() // Bu, tüm parçaları bir listeye alacak
                        val relativePaths = mutableListOf<String>()

                        for (part in parts) {
                            when (part) {
                                is PartData.FileItem -> {
                                    val ext = File(part.originalFileName!!).extension
                                    val fileBytes = part.streamProvider().readBytes()

                                    if (folderName == null) {
                                        call.respond(
                                            HttpStatusCode.BadRequest,
                                            ErrorResponse(21001, "folderName is missing")
                                        )
                                        MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   /postWebFolders -> folderName is missing in request", ContextCompat.getColor(applicationContext, R.color.custom_red), false)
                                        return@post
                                    }

                                    val basePath = File(
                                        applicationContext.getExternalFilesDir(null),
                                        folderName
                                    )

                                    // İlgili yolu al
                                    val relativePath =
                                        relativePaths.removeAt(0) // İlk elemanı al ve kaldır

                                    // Ana klasör ismini yoldan çıkar
                                    val cleanedPath = relativePath.replace("$folderName/", "")

                                    val fullPath = File(basePath, cleanedPath)

                                    if (!fullPath.parentFile.exists()) {
                                        if (!fullPath.parentFile.mkdirs()) {
                                            Log.i(
                                                TAG,
                                                "Failed to create directory: ${fullPath.parentFile}"
                                            )
                                            call.respond(
                                                HttpStatusCode.InternalServerError,
                                                ErrorResponse(21002, "Failed to create directory")
                                            )
                                            MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   /postWebFolders -> Failed to create directory", ContextCompat.getColor(applicationContext, R.color.custom_red), false)
                                            return@post
                                        }
                                    }

                                    fullPath.writeBytes(fileBytes)

                                }

                                is PartData.FormItem -> {
                                    if (part.name == "folderName") {
                                        folderName = part.value
                                        Log.i(TAG, "/postWebFolders folderName: $folderName")
                                    } else if (part.name == "filePaths[]") {
                                        relativePaths.add(part.value)
                                    }

                                }

                                else -> {}
                            }
                            part.dispose()
                        }

                        if (folderName == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(21001, "folderName is missing")
                            )
                            MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   /postWebFolders -> folderName is missing in request", ContextCompat.getColor(applicationContext, R.color.custom_red), false)
                            return@post
                        }


                        val database = AppDatabase.getDatabase(applicationContext)
                        val dao = database.folderDao()
                        val totalData = dao.getAll().size
                        val isSelected = totalData == 0
                        Log.i(
                            TAG,
                            " totalData == 0: " + (totalData == 0) + " isSelected: " + isSelected + " totalData:  " + totalData
                        )
                        val customServerFolders = CustomServerFolders(
                            id = 0,
                            folderName = folderName,
                            isSelected = isSelected
                        )
                        dao.insert(customServerFolders)

                        Log.i(TAG, "/postWebFolders  call.respond Files uploaded successfully")
                        val token = makeToken()
                        call.response.cookies.append(Cookie("auth_token", token, path = "/", httpOnly = true, maxAge = 3600)) // 1 saat
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf("message" to "Files uploaded successfully")
                        )
                        MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   /postWebFolders -> Files retrieved successfully", ContextCompat.getColor(applicationContext, R.color.custom_green), false)

                    } catch (e: Exception) {
                        Log.i(TAG, "/postWebFolders ERROR: ")
                        MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   /postWebFolders -> Something happened", ContextCompat.getColor(applicationContext, R.color.custom_red), false)
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(21003, "Internal Server Error")
                        )
                    }
                }
            }



            post("/postAuthPassword") {
                try {
                    val requestData = call.receiveText()
                    Log.i(TAG, "Received data in /postAuthPassword -> : $requestData")

                    MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   POST AUTH PASSWORD REQUEST", Color.WHITE, false)

                    // Gson ile JSON'dan haritaya dönüşüm yap
                    val data: Map<String, String> =
                        Gson().fromJson(requestData, Map::class.java) as Map<String, String>
                    val clientPassword = data["password"]

                    val serverPasswordEncrypted =
                        sha256(DefaultServerData.defaultServerAuthPassword.toString())  // sunucudaki şifreyi şifreleyin

                    if (clientPassword == serverPasswordEncrypted) {
                        // Şifre doğru
                        Log.i(TAG, "Şifre Doğru")
                        MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   /postAuthPassword -> CORRECT PASSWORD", ContextCompat.getColor(applicationContext, R.color.custom_green), false)
                        val token = makeToken()
                        call.response.cookies.append(
                            Cookie(
                                "auth_token",
                                token,
                                path = "/",
                                httpOnly = true,
                                maxAge = 3600
                            )
                        ) // 1 saat
                        call.respond(ResponseDto(true, token))
                    } else {
                        // Şifre yanlış
                        Log.i(TAG, "Şifre yanlış")
                        MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   /postAuthPassword -> INCORRECT PASSWORD", ContextCompat.getColor(applicationContext, R.color.custom_red), false)

                        call.respond(ResponseDto(false))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "postAuthPassword HATA : ", e)
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("password_status" to "Failed to process request")
                    )
                }
            }


            //ROUTE CODE 03
            authenticate ("auth-jwt"){
                get ("/getCurrentFolderData"){
                    MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()}   GET (/getCurrentFolderData) REQUEST", Color.WHITE, false)

                    val database = AppDatabase.getDatabase(applicationContext)
                    val dao = database.folderDao()
                    val folderNames = dao.getSelectedFolder()

                    if(folderNames != null){
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf("selected_folder" to folderNames.folderName, "selected_starter_page" to folderNames.selectedFile)
                        )
                    }else{
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(31001, "Can't find any selected folder and starter page.")
                        )
                    }
                }
            }


        }
        Log.i(TAG, "server() ->")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        server.start(wait = false)
        Log.i(TAG, "onStartCommand() -> ")
        MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()} DefaultServer is started.", ContextCompat.getColor(applicationContext, R.color.custom_green), false)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        server.stop(1000, 5000)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2)

        Log.i(TAG, "onDestroy() ->")
        MainActivity.getInstance()?.addLogFromInstance("DS ${MainActivity.getInstance()?.getCurrentTime()} DefaultServer is stopped.", ContextCompat.getColor(applicationContext, R.color.custom_red), false)

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

    @Serializable
    data class ErrorResponse(val error_code: Int, val message: String)

}