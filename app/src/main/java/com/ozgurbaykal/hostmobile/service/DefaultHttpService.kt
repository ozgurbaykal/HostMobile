package com.ozgurbaykal.hostmobile.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
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
import com.ozgurbaykal.hostmobile.control.DatabaseHelper
import com.ozgurbaykal.hostmobile.control.DefaultServerSharedPreferenceManager
import com.ozgurbaykal.hostmobile.view.MainActivity
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.routing.delete
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

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
            methods += HttpMethod.Delete
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
                    MainActivity.getInstance()?.addLogFromInstance(
                        "DS ${
                            MainActivity.getInstance()?.getCurrentTime()
                        }   JWT token is null in the request to the server. Redirect to home page. ",
                        ContextCompat.getColor(applicationContext, R.color.custom_red),
                        false
                    )
                    MainActivity.getInstance()?.addLogFromInstance(
                        "DS ${
                            MainActivity.getInstance()?.getCurrentTime()
                        }   Request ->  $requestPath", Color.WHITE, false
                    )

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
                            val fileContent =
                                applicationContext.assets.open(requestPath).use { it.readBytes() }
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

                    MainActivity.getInstance()?.addLogFromInstance(
                        "DS ${
                            MainActivity.getInstance()?.getCurrentTime()
                        }   JWT token is true.",
                        ContextCompat.getColor(applicationContext, R.color.custom_green),
                        false
                    )
                    MainActivity.getInstance()?.addLogFromInstance(
                        "DS ${
                            MainActivity.getInstance()?.getCurrentTime()
                        }   Request ->  $requestPath", Color.WHITE, false
                    )

                    val assetManager = applicationContext.assets

                    if (requestPath.isEmpty()) {

                        Log.i(TAG, "TOKEN ARTIK DOĞRU, GİRİŞ SAYFASI YERİNE ANA SAYFAYI GÖSTER")
                        val authContent =
                            assetManager.open("DefaultServerWeb/default-server-main.html")
                                .use { it.readBytes() }
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
                        call.respondBytes(authContent, ContentType.Text.Html)
                        return@get
                    } else {
                        try {

                            //BU SERVİSTE TÜM DOSYALAR DefaultServerWeb KLASÖRÜ İÇERİSİNDE OLACAĞI İÇİN KLASÖR İSMİNİ BAŞA EKLİYORUZ
                            if (!requestPath.contains("../"))
                                requestPath = "DefaultServerWeb/" + requestPath

                            Log.i(
                                TAG,
                                "default-server-main.html için ELSE bloğuna girdi requestPath: " + requestPath
                            )

                            val contentType = getContentType(File(requestPath))

                            val fileContent = assetManager.open(requestPath).use { it.readBytes() }
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
                            call.respondBytes(fileContent, contentType)
                            return@get
                        } catch (e: IOException) {
                            Log.e(TAG, "DOSYA BULAMADI")
                            // Dosya bulunamadıysa bu bloğa girecektir.
                        }
                    }


                } catch (e: JWTVerificationException) {

                    MainActivity.getInstance()?.addLogFromInstance(
                        "DS ${
                            MainActivity.getInstance()?.getCurrentTime()
                        }   Can't verify JWT token, redirect to home page.",
                        ContextCompat.getColor(applicationContext, R.color.custom_red),
                        false
                    )
                    MainActivity.getInstance()?.addLogFromInstance(
                        "DS ${
                            MainActivity.getInstance()?.getCurrentTime()
                        }   Request ->  $requestPath", Color.WHITE, false
                    )

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
                            val fileContent =
                                applicationContext.assets.open(requestPath).use { it.readBytes() }
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
                    MainActivity.getInstance()?.addLogFromInstance(
                        "DS ${
                            MainActivity.getInstance()?.getCurrentTime()
                        }   POST WEB FOLDERS REQUEST", Color.WHITE, false
                    )

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
                                        MainActivity.getInstance()?.addLogFromInstance(
                                            "DS ${
                                                MainActivity.getInstance()?.getCurrentTime()
                                            }   /postWebFolders -> folderName is missing in request",
                                            ContextCompat.getColor(
                                                applicationContext,
                                                R.color.custom_red
                                            ),
                                            false
                                        )
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
                                            MainActivity.getInstance()?.addLogFromInstance(
                                                "DS ${
                                                    MainActivity.getInstance()?.getCurrentTime()
                                                }   /postWebFolders -> Failed to create directory",
                                                ContextCompat.getColor(
                                                    applicationContext,
                                                    R.color.custom_red
                                                ),
                                                false
                                            )
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
                            MainActivity.getInstance()?.addLogFromInstance(
                                "DS ${
                                    MainActivity.getInstance()?.getCurrentTime()
                                }   /postWebFolders -> folderName is missing in request",
                                ContextCompat.getColor(applicationContext, R.color.custom_red),
                                false
                            )
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
                        call.response.cookies.append(
                            Cookie(
                                "auth_token",
                                token,
                                path = "/",
                                httpOnly = true,
                                maxAge = 3600
                            )
                        ) // 1 saat
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf("message" to "Files uploaded successfully")
                        )
                        MainActivity.getInstance()?.addLogFromInstance(
                            "DS ${
                                MainActivity.getInstance()?.getCurrentTime()
                            }   /postWebFolders -> Files retrieved successfully",
                            ContextCompat.getColor(applicationContext, R.color.custom_green),
                            false
                        )

                    } catch (e: Exception) {
                        Log.i(TAG, "/postWebFolders ERROR: ")
                        MainActivity.getInstance()?.addLogFromInstance(
                            "DS ${
                                MainActivity.getInstance()?.getCurrentTime()
                            }   /postWebFolders -> Something happened",
                            ContextCompat.getColor(applicationContext, R.color.custom_red),
                            false
                        )
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

                    MainActivity.getInstance()?.addLogFromInstance(
                        "DS ${
                            MainActivity.getInstance()?.getCurrentTime()
                        }   POST AUTH PASSWORD REQUEST", Color.WHITE, false
                    )

                    // Gson ile JSON'dan haritaya dönüşüm yap
                    val data: Map<String, String> =
                        Gson().fromJson(requestData, Map::class.java) as Map<String, String>
                    val clientPassword = data["password"]

                    val serverPasswordEncrypted =
                        sha256(DefaultServerData.defaultServerAuthPassword.toString())  // sunucudaki şifreyi şifreleyin

                    if (clientPassword == serverPasswordEncrypted) {
                        // Şifre doğru
                        Log.i(TAG, "Şifre Doğru")
                        MainActivity.getInstance()?.addLogFromInstance(
                            "DS ${
                                MainActivity.getInstance()?.getCurrentTime()
                            }   /postAuthPassword -> CORRECT PASSWORD",
                            ContextCompat.getColor(applicationContext, R.color.custom_green),
                            false
                        )
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
                        MainActivity.getInstance()?.addLogFromInstance(
                            "DS ${
                                MainActivity.getInstance()?.getCurrentTime()
                            }   /postAuthPassword -> INCORRECT PASSWORD",
                            ContextCompat.getColor(applicationContext, R.color.custom_red),
                            false
                        )

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
            authenticate("auth-jwt") {
                get("/getCurrentFolderData") {
                    MainActivity.getInstance()?.addLogFromInstance(
                        "DS ${
                            MainActivity.getInstance()?.getCurrentTime()
                        }   GET (/getCurrentFolderData) REQUEST", Color.WHITE, false
                    )

                    val database = AppDatabase.getDatabase(applicationContext)
                    val dao = database.folderDao()
                    val folderNames = dao.getSelectedFolder()

                    if (folderNames != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf(
                                "selected_folder" to folderNames.folderName,
                                "selected_starter_page" to folderNames.selectedFile
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(31001, "Can't find any selected folder and starter page.")
                        )
                    }
                }
            }

            //SHAREDPREFENCE API STRING WRITE-READ
            authenticate("auth-jwt") {
                //ROUTE CODE 04
                post("/sharedpreference/write/string") {

                    try {
                        val requestData = call.receive<PreferenceDataString>()
                        Log.i(TAG, "key: ${requestData.key} , value: ${requestData.value}")

                        DefaultServerSharedPreferenceManager.writeString(
                            requestData.key,
                            requestData.value
                        )

                        call.respond(HttpStatusCode.OK, "SharedPreference saved.")
                    } catch (e: Exception) {
                        when (e) {
                            is BadRequestException -> {
                                // Deserializasyon hatası için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse(
                                        41001,
                                        "Deserialization error. Check the format of your request data."
                                    )
                                )
                            }

                            is IOException -> {
                                // I/O hataları için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        41002,
                                        "Internal server error. Please try again later."
                                    )
                                )
                            }

                            else -> {
                                // Genel hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        41003,
                                        "Something unexpected happened on the server."
                                    )
                                )
                            }
                        }
                    }
                }

                //ROUTE CODE 05
                get("/sharedpreference/read/string") {

                    val key = call.parameters["key"]
                    val defaultStringValue = call.parameters["defaultValue"]

                    if (key != null && defaultStringValue != null) {
                        Log.i(TAG, "KEY: $key")


                        call.respond(
                            HttpStatusCode.OK,
                            SuccessResponse(
                                true,
                                DefaultServerSharedPreferenceManager.readString(
                                    key,
                                    defaultStringValue
                                ).toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                51001,
                                "Please send all parameters. key -> Your data key.  ---  defaultValue -> If data is null return default value."
                            )
                        )
                    }
                }

            }

            //SHAREDPREFENCE API INTEGER WRITE-READ
            authenticate("auth-jwt") {
                //ROUTE CODE 06
                post("/sharedpreference/write/int") {

                    try {
                        val requestData = call.receive<PreferenceDataInteger>()
                        Log.i(TAG, "key: ${requestData.key} , value: ${requestData.value}")

                        DefaultServerSharedPreferenceManager.writeInteger(
                            requestData.key,
                            requestData.value
                        )

                        call.respond(HttpStatusCode.OK, "SharedPreference saved.")
                    } catch (e: Exception) {
                        Log.e(TAG, "/sharedpreference/write/int ERROR-> " + e.message)
                        when (e) {
                            is BadRequestException -> {
                                // Deserializasyon hatası için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse(
                                        61001,
                                        "Deserialization error. Check the format of your request data."
                                    )
                                )
                            }

                            is IOException -> {
                                // I/O hataları için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        61002,
                                        "Internal server error. Please try again later."
                                    )
                                )
                            }

                            else -> {
                                // Genel hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        61003,
                                        "Something unexpected happened on the server."
                                    )
                                )
                            }
                        }
                    }
                }

                //ROUTE CODE 07
                get("/sharedpreference/read/int") {

                    val key = call.parameters["key"]
                    val defaultIntegerValue = call.parameters["defaultValue"]

                    if (key != null && defaultIntegerValue != null) {
                        Log.i(TAG, "KEY: $key")

                        call.respond(
                            HttpStatusCode.OK,
                            SuccessResponse(
                                true,
                                DefaultServerSharedPreferenceManager.readInteger(
                                    key,
                                    defaultIntegerValue.toInt()
                                ).toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                71001,
                                "Please send all parameters. key -> Your data key.  ---  defaultValue -> If data is null return default value."
                            )
                        )
                    }
                }

            }

            //SHAREDPREFENCE API BOOLEAN WRITE-READ
            authenticate("auth-jwt") {
                //ROUTE CODE 08
                post("/sharedpreference/write/boolean") {

                    try {
                        val requestData = call.receive<PreferenceDataBoolean>()
                        Log.i(TAG, "key: ${requestData.key} , value: ${requestData.value}")

                        DefaultServerSharedPreferenceManager.writeBoolean(
                            requestData.key,
                            requestData.value
                        )

                        call.respond(HttpStatusCode.OK, "SharedPreference saved.")
                    } catch (e: Exception) {
                        Log.e(TAG, "/sharedpreference/write/boolean ERROR-> " + e.message)
                        when (e) {
                            is BadRequestException -> {
                                // Deserializasyon hatası için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse(
                                        81001,
                                        "Deserialization error. Check the format of your request data."
                                    )
                                )
                            }

                            is IOException -> {
                                // I/O hataları için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        81002,
                                        "Internal server error. Please try again later."
                                    )
                                )
                            }

                            else -> {
                                // Genel hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        81003,
                                        "Something unexpected happened on the server."
                                    )
                                )
                            }
                        }
                    }
                }


                //ROUTE CODE 09
                get("/sharedpreference/read/boolean") {

                    val key = call.parameters["key"]
                    val defaultBooleanValue = call.parameters["defaultValue"]

                    if (key != null && defaultBooleanValue != null) {
                        Log.i(TAG, "KEY: $key")


                        call.respond(
                            HttpStatusCode.OK,
                            SuccessResponse(
                                true,
                                DefaultServerSharedPreferenceManager.readBoolean(
                                    key,
                                    defaultBooleanValue.toBoolean()
                                ).toString()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                91001,
                                "Please send all parameters. key -> Your data key.  ---  defaultBooleanValue -> If data is null return default value."
                            )
                        )
                    }
                }

            }


            //SHAREDPREFENCE API REMOVE DATA
            authenticate("auth-jwt") {
                //ROUTE CODE 08
                delete("/sharedpreference/remove") {
                    val key = call.parameters["key"]
                    Log.i(TAG, " key: $key")
                    if (key != null) {

                        DefaultServerSharedPreferenceManager.remove(key)

                        call.respond(
                            HttpStatusCode.OK,
                            SuccessResponse(true, "SharedPreference removed.")
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                81001,
                                "Please send all parameters. key -> Your data key."
                            )
                        )
                    }

                }



                //ROUTE CODE 10
                delete("/sharedpreference/remove/all") {
                    try {
                        DefaultServerSharedPreferenceManager.removeAll()

                        call.respond(
                            HttpStatusCode.OK,
                            SuccessResponse(true, "All SharedPreference removed.")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "/sharedpreference/remove/all -> error: " + e.message)

                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(101001, "Internal Server Error")
                        )
                    }

                }
            }

            //CREATE-IS SQLITE DATABASE ROUTES
            authenticate("auth-jwt") {
                //ROUTE CODE 11
                post("/sqlite/create/database") {

                    try {
                        val requestData = call.receive<DatabaseCreate>()
                        Log.i(TAG, "database_name: ${requestData.database_name} ")

                        try {
                            val dbHelper = DatabaseHelper(applicationContext, requestData.database_name.replace(" ", ""))

                            Log.i(TAG, "db name after create: ${dbHelper.databaseName}")

                            call.respond(
                                HttpStatusCode.OK,
                                SuccessResponse(true, "The database named ${requestData.database_name} has been created.")
                            )
                        } catch (e: Exception) {
                            // when create db something happend
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(
                                    111001,
                                    "Something happened when try create database. SERVER MESSAGE: ${e.message}"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "/sqlite/create/database ERROR-> " + e.message)
                        when (e) {
                            is BadRequestException -> {
                                // Deserializasyon hatası için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse(
                                        111002,
                                        "Deserialization error. Check the format of your request data. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            is IOException -> {
                                // I/O hataları için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        111003,
                                        "Internal server error. Please try again later. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            else -> {
                                // Genel hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        111004,
                                        "Something unexpected happened on the server. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }
                        }
                    }
                }

                //ROUTE CODE 12
                get("/sqlite/is/database/exist") {

                    val db_name = call.parameters["database_name"]

                    if (db_name != null) {
                        Log.i(TAG, "/sqlite/is/database/exist -> db_name: $db_name")

                        val dbFile = applicationContext.getDatabasePath(db_name.toString().replace(" ", ""))

                        call.respond(
                            HttpStatusCode.OK,
                            SuccessResponseBoolean(true, dbFile.exists())
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                121001,
                                "Please send all parameters. database_name -> Your database name."
                            )
                        )
                    }
                }
            }


            //CREATE-DELETE-IS SQLITE TABLE ROUTES
            authenticate("auth-jwt") {
                //ROUTE CODE 13
                post("/sqlite/create/table") {

                    try {
                        val requestData = call.receive<TableCreate>()
                        Log.i(TAG, "table_create_query: ${requestData.table_create_query} ")

                        try {
                            val dbHelper = DatabaseHelper(applicationContext, requestData.database_name.replace(" ", ""))

                            dbHelper.createTable(requestData.table_create_query.toString())

                            call.respond(
                                HttpStatusCode.OK,
                                SuccessResponse(true, "The table has been created.")
                            )
                        } catch (e: Exception) {
                            // when create db something happend
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(
                                    131001,
                                    "Something happened when try create database. SERVER MESSAGE: ${e.message}"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "/sqlite/create/table ERROR-> " + e.message)
                        when (e) {
                            is BadRequestException -> {
                                // Deserializasyon hatası için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse(
                                        131002,
                                        "Deserialization error. Check the format of your request data. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            is IOException -> {
                                // I/O hataları için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        131003,
                                        "Internal server error. Please try again later. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            else -> {
                                // Genel hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        131004,
                                        "Something unexpected happened on the server. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }
                        }
                    }
                }

                //ROUTE CODE 14
                delete("/sqlite/delete/table") {
                    //ROUTE CODE 14
                    try {
                        val requestData = call.receive<DropTable>()
                        Log.i(TAG, "/sqlite/delete/table -> table_name: ${requestData.table_name} ")

                        val dbHelper = DatabaseHelper(applicationContext, requestData.database_name.replace(" ", ""))
                        dbHelper.dropTable(requestData.table_name.replace(" ", ""))

                        call.respond(
                            HttpStatusCode.OK,
                            SuccessResponse(true, "The table named ${requestData.table_name.replace(" ", "")} has been deleted.")
                        )

                    } catch (e: Exception) {
                        Log.e(TAG, "/sqlite/delete/table -> error: " + e.message)

                        when (e) {
                            is BadRequestException -> {
                                // Deserializasyon hatası için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse(
                                        141002,
                                        "Deserialization error. Check the format of your request data. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            is IOException -> {
                                // I/O hataları için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        141003,
                                        "Internal server error. Please try again later. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            else -> {
                                // Genel hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        141004,
                                        "Something unexpected happened on the server. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }
                        }
                    }
                }



                //ROUTE CODE 15
                get("/sqlite/is/table/exist") {

                    try {
                        val requestData = call.receive<IsTableExist>()
                        Log.i(TAG, "/sqlite/is/table/exist -> table_name: ${requestData.table_name} ")


                        val dbHelper = DatabaseHelper(applicationContext, requestData.database_name.replace(" ", ""))
                        val isTableExist = dbHelper.tableExists(dbHelper.readableDatabase, requestData.table_name)

                        call.respond(
                            HttpStatusCode.OK,
                            SuccessResponseBoolean(true, isTableExist)
                        )

                    } catch (e: Exception) {
                        Log.e(TAG, "/sqlite/is/table/exist -> error: " + e.message)

                        when (e) {
                            is BadRequestException -> {
                                // Deserializasyon hatası için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse(
                                        151002,
                                        "Deserialization error. Check the format of your request data. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            is IOException -> {
                                // I/O hataları için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        151003,
                                        "Internal server error. Please try again later. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            else -> {
                                // Genel hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        151004,
                                        "Something unexpected happened on the server. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }
                        }
                    }
                }






            }

            //DELETE DATABASE AND RUN QUERY ROUTES
            authenticate ("auth-jwt"){

                //ROUTE CODE 16
                post("/sqlite/query") {

                    try {
                        val requestData = call.receive<QueryRequest>()

                        val dbHelper = DatabaseHelper(applicationContext, requestData.database_name.replace(" ", ""))

                        dbHelper.writableDatabase.execSQL(requestData.query)
                        call.respond(HttpStatusCode.OK, SuccessResponse(true, "Query executed successfully"))

                    } catch (e: Exception) {
                        Log.e(TAG, "/sqlite/is/table/exist -> error: " + e.message)

                        when (e) {
                            is BadRequestException -> {
                                // Deserializasyon hatası için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse(
                                        161002,
                                        "Deserialization error. Check the format of your request data. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            is IOException -> {
                                // I/O hataları için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        161003,
                                        "Internal server error. Please try again later. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            else -> {
                                // Genel hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        161004,
                                        "Something unexpected happened on the server. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }
                        }
                    }
                }


                //ROUTE CODE 17
                delete("/sqlite/delete/database") {

                    try {
                        val requestData = call.receive<DatabaseReset>()

                        val dbHelper = DatabaseHelper(applicationContext, requestData.database_name.replace(" ", ""))
                        dbHelper.deleteDatabase(requestData.database_name.replace(" ", ""))

                        call.respond(HttpStatusCode.OK, SuccessResponse(true, "Database deleted successfully"))

                    } catch (e: Exception) {
                        Log.e(TAG, "/sqlite/delete/database -> error: " + e.message)

                        when (e) {
                            is BadRequestException -> {
                                // Deserializasyon hatası için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse(
                                        171002,
                                        "Deserialization error. Check the format of your request data. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            is IOException -> {
                                // I/O hataları için spesifik bir hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        171003,
                                        "Internal server error. Please try again later. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }

                            else -> {
                                // Genel hata mesajı
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse(
                                        171004,
                                        "Something unexpected happened on the server. SERVER MESSAGE: ${e.message}"
                                    )
                                )
                            }
                        }
                    }
                }
            }



        }
        Log.i(TAG, "server() ->")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        server.start(wait = false)
        Log.i(TAG, "onStartCommand() -> ")
        MainActivity.getInstance()?.addLogFromInstance(
            "DS ${
                MainActivity.getInstance()?.getCurrentTime()
            } DefaultServer is started.",
            ContextCompat.getColor(applicationContext, R.color.custom_green),
            false
        )

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        server.stop(1000, 5000)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2)

        Log.i(TAG, "onDestroy() ->")
        MainActivity.getInstance()?.addLogFromInstance(
            "CS ${
                MainActivity.getInstance()?.getCurrentTime()
            } Default is stopped.",
            ContextCompat.getColor(applicationContext, R.color.custom_red),
            false
        )

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
    data class SuccessResponse(val success: Boolean, val message: String)

    @Serializable
    data class SuccessResponseBoolean(val success: Boolean, val message: Boolean)

    @Serializable
    data class ErrorResponse(val error_code: Int, val message: String)

    @Serializable
    data class PreferenceDataString(val key: String, val value: String)

    @Serializable
    data class PreferenceDataInteger(val key: String, val value: Int)

    @Serializable
    data class PreferenceDataBoolean(val key: String, val value: Boolean)

    @Serializable
    data class DatabaseCreate(val database_name: String)

    @Serializable
    data class DatabaseReset(val database_name: String)
    @Serializable
    data class TableCreate(val database_name: String, val table_create_query: String)

    @Serializable
    data class DropTable(val database_name: String, val table_name: String)

    @Serializable
    data class IsTableExist(val database_name: String, val table_name: String)

    @Serializable
    data class QueryRequest(
        val database_name: String,
        val query: String
    )

}