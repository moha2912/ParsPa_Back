package example.com

import example.com.plugins.*
import io.ktor.server.application.*
import java.text.DecimalFormat
import java.util.*

const val USERS_FOLDER = "users/"
const val MAIN_DOMAIN = "https://parspa-ai.ir/"
const val API_DOMAIN = "https://api.parspa-ai.ir/"
const val PAYMENT_ROUTE = "payment"
const val PAYMENT_ADDRESS = "$API_DOMAIN$PAYMENT_ROUTE"

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
const val ZIBAL_MERCHANT = "66e5b33c6f3803001dcebea1"

const val INSOLE_PRICE = 1500000L
val INSOLE_PRICE_FORMATTED = DecimalFormat
    .getInstance()
    .format(INSOLE_PRICE)
    .plus(" تومان")

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
