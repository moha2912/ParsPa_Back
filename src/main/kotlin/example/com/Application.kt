package example.com

import example.com.plugins.*
import io.ktor.server.application.*

const val USERS_FOLDER = "pars-pa\\"

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
    //todo recursive remove otps in 10 min
    //todo recursive remove unused images in 10 min
}

fun Application.module() {
    configureStatusPages()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureSecurity()
    configureRouting()
}
