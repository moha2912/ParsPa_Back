package example.com

import example.com.plugins.*
import io.ktor.server.application.*
import java.io.File
import java.util.*

const val USERS_FOLDER = "users/"

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tehran"))
    io.ktor.server.netty.EngineMain.main(args)
    //File(USERS_FOLDER).mkdir()
    //todo recursive remove otps in 10 min (otp doesnt need database table)
    //todo recursive remove unused images in 10 min
}

fun Application.module() {
    configureRateLimit()
    configureStatusPages()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureSecurity()
    configureRouting()
}
