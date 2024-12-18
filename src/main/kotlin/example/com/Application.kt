package example.com

import example.com.plugins.*
import io.ktor.server.application.*
import java.util.*

var isDebug: Boolean = false
var env: String = "dsvfb"
const val USERS_FOLDER = "users/"
const val MAIN_DOMAIN = "https://parspa-ai.ir/"
const val API_DOMAIN = "https://api.parspa-ai.ir/"
const val DEBUG_DOMAIN = "https://dev.parspa-ai.ir/"
const val PAYMENT_ROUTE = "payment"
const val PRD_PAYMENT_ADDRESS = "$API_DOMAIN$PAYMENT_ROUTE"
const val DEV_PAYMENT_ADDRESS = "$DEBUG_DOMAIN$PAYMENT_ROUTE"

const val DL_HOST = "https://dl.parspa-ai.ir/"
const val DL_PATH = "/var/www/downloads/"

const val DEEPLINK_ERROR = "${MAIN_DOMAIN}payment.php"
const val DEEPLINK_SUCCESS = "${MAIN_DOMAIN}payment.php"

const val SMS_PATTERN_URL = "https://api2.ippanel.com/api/v1/sms/pattern/normal/send"
const val SMS_NORMAL_URL = "https://api2.ippanel.com/api/v1/sms/send/webservice/single"
const val SMS_PANEL_API = "UP63w9369jXeDkCxJBr1FmVkk6QHYP2S1aKmrxLHo-E="

const val ZIBAL_REQUEST_URL = "https://gateway.zibal.ir/v1/request"
const val ZIBAL_START_URL = "https://gateway.zibal.ir/start/"
const val ZIBAL_VERIFY_URL = "https://gateway.zibal.ir/v1/verify"
val ZIBAL_MERCHANT
    get() = if (isDebug) "zibal" else "66e5b33c6f3803001dcebea1"
val PAYMENT_ADDRESS
    get() = if (isDebug) DEV_PAYMENT_ADDRESS else PRD_PAYMENT_ADDRESS

fun main(args: Array<String>) {
    isDebug = true
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tehran"))
    io.ktor.server.netty.EngineMain.main(args)
    //todo recursive remove otps in 10 min (otp doesnt need database table)
    //todo recursive remove unused images in 10 min
}

fun Application.module() {
    env = environment.config
        .propertyOrNull("ktor.environment")
        ?.getString() ?: "sdvf"
    isDebug = env == "development"
    TelegramBot.prepare()
    configureCors()
    configureRateLimit()
    configureStatusPages()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureSecurity()
    configureRouting()
}
