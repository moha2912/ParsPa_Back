package example.com

import example.com.plugins.*
import io.ktor.server.application.*
import java.util.*

const val USERS_FOLDER = "users/"
const val DL_HOST = "https://dl.parspa-ai.ir/"
const val DL_PATH = "/var/www/downloads/"

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tehran"))
    io.ktor.server.netty.EngineMain.main(args)
    //todo recursive remove otps in 10 min (otp doesnt need database table)
    //todo recursive remove unused images in 10 min
}

fun Application.module() {
    // todo TelegramBot.prepare()
    configureCors()
    configureRateLimit()
    configureStatusPages()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureSecurity()
    configureRouting()
}
