package example.com.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import example.com.*
import example.com.data.model.*
import example.com.data.model.exception.AuthorizationException
import example.com.data.schema.AdminUserService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

val EXPIRE_TIME = TimeUnit.DAYS.toMillis(180L)
private const val CLAIM_KEY = "id"
private const val ADMIN_KEY = "admin"
private const val jwtAudience = "jwt-audience"
private const val jwtDomain = "https://jwt-provider-domain/"
private const val jwtSecret = "\$S-d{xu-HG2V5OzuVDH~@rg1=lz+!V\$mjOBD\$Q%Xa#hptvI\$!?wf}_"

fun Application.configureSecurity() {
    val jwtRealm = "ParsPa-AI"
    authentication {
        jwt {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

fun createToken(tokenClaim: TokenClaim): String? {
    return JWT
        .create()
        .withAudience(jwtAudience)
        .withIssuer(jwtDomain)
        .withClaim(CLAIM_KEY, tokenClaim.userID /*todo Json.encodeToString(tokenClaim)*/)
        .withExpiresAt(
            Date(
                System
                    .currentTimeMillis()
                    .plus(EXPIRE_TIME)
            )
        )
        .sign(Algorithm.HMAC256(jwtSecret))
}

fun createAdminToken(): String? {
    return JWT
        .create()
        .withAudience(jwtAudience)
        .withIssuer(jwtDomain)
        .withClaim(ADMIN_KEY, true)
        .sign(Algorithm.HMAC256(jwtSecret))
}

fun PipelineContext<Unit, ApplicationCall>.getIdFromToken(): Long {
    /*todo try {
        val principal = call.principal<JWTPrincipal>()
        val claim: TokenClaim = Json.decodeFromString(
            principal?.payload
                ?.getClaim(CLAIM_KEY)
                .toString()
        )
        return claim.userID
    } catch (e: Exception) {
        e.printStackTrace()
    }
    throw AuthorizationException()*/
    val principal = call.principal<JWTPrincipal>()
    return principal?.payload
        ?.getClaim(CLAIM_KEY)
        ?.asLong() ?: throw AuthorizationException()
}

suspend fun PipelineContext<Unit, ApplicationCall>.checkAdminUser(adminUserService: AdminUserService) {
    val id = getIdFromToken()
    val user = adminUserService.readID(id) ?: throw AuthorizationException()
    if (user.isActive != true) {
        throw BadRequestException("Not active")
    }
}

fun PipelineContext<Unit, ApplicationCall>.getQueryParameter(path: String) = call.request.queryParameters[path]
fun PipelineContext<Unit, ApplicationCall>.getPathParameter(path: String) = call.parameters[path]
fun PipelineContext<Unit, ApplicationCall>.getHeader(header: String) = call.request.header(header)
fun PipelineContext<Unit, ApplicationCall>.getBoolHeader(header: String) = call.request
    .header(header)
    .toBoolean()

fun PipelineContext<Unit, ApplicationCall>.getFlavorHeader() =
    Flavor.valueOf((call.request.header("flavor") ?: "persian").uppercase())

fun PipelineContext<Unit, ApplicationCall>.getBrandHeader() = call.request.header("brand")
fun PipelineContext<Unit, ApplicationCall>.getModelHeader() = call.request.header("model")
fun PipelineContext<Unit, ApplicationCall>.getApiHeader() = call.request
    .header("api")
    ?.toIntOrNull()

fun PipelineContext<Unit, ApplicationCall>.getUserAgent() = call.request.header("User-Agent")

inline fun <reified T : Any> sendPostRequest(
    url: String,
    body: T,
    headers: Map<String, String> = emptyMap(),
): Boolean {
    try {
        val requestBodyJson = Json.encodeToString(body)
        val urlConnection = URL(url).openConnection() as HttpURLConnection
        urlConnection.requestMethod = "POST"
        urlConnection.setRequestProperty("Content-Type", "application/json")
        headers.forEach {
            urlConnection.setRequestProperty(it.key, it.value)
        }
        urlConnection.useCaches = false
        urlConnection.doInput = true
        urlConnection.doOutput = true
        urlConnection.outputStream.use { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                writer.write(requestBodyJson)
                writer.flush()
            }
        }
        urlConnection.connect()
        if (urlConnection.responseCode in 200..299) {
            try {
                val responseJson = urlConnection.inputStream
                    .bufferedReader()
                    .readText()
                println("Response: $responseJson")
                return true
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }
        val errorResponse = urlConnection.errorStream
            ?.bufferedReader()
            ?.readText()
        println("Error response: $errorResponse")
    } catch (e: Exception) {
        println(e.stackTraceToString())
    }
    return false
}

val json = Json {
    ignoreUnknownKeys = true
}

inline fun <reified T : Any, reified M : Any> sendPostRequestModel(
    url: String,
    body: T,
    headers: Map<String, String> = emptyMap(),
): M? {
    try {
        val requestBodyJson = Json.encodeToString(body)
        val urlConnection = URL(url).openConnection() as HttpURLConnection
        urlConnection.requestMethod = "POST"
        urlConnection.setRequestProperty("Content-Type", "application/json")
        headers.forEach {
            urlConnection.setRequestProperty(it.key, it.value)
        }
        urlConnection.useCaches = false
        urlConnection.doInput = true
        urlConnection.doOutput = true
        urlConnection.outputStream.use { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                writer.write(requestBodyJson)
                writer.flush()
            }
        }
        urlConnection.connect()
        if (urlConnection.responseCode in 200..299) {
            try {
                val responseJson = urlConnection.inputStream
                    .bufferedReader()
                    .readText()
                return json.decodeFromString(responseJson)
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }
        val errorResponse = urlConnection.errorStream
            ?.bufferedReader()
            ?.readText()
        println("Error response: $errorResponse")
    } catch (e: Exception) {
        println(e.stackTraceToString())
    }
    return null
}

fun createPayRequest(
    merchant: String,
    amount: Long,
    desc: String,
    phone: String
): ZibalRequestStatus? = sendPostRequestModel(
    url = ZIBAL_REQUEST_URL,
    body = ZibalRequest(
        amount = amount,
        callbackUrl = PAYMENT_ADDRESS,
        description = desc,
        merchant = merchant,
        mobile = phone
    )
)

fun verifyPayment(
    trackID: Long,
    merchant: String,
): ZibalVerifyResponse? = sendPostRequestModel(
    url = ZIBAL_VERIFY_URL,
    body = ZibalVerify(
        merchant = merchant,
        trackId = trackID
    )
)

fun sendOTPRequest(recipient: String, otp: Int, hash: String): Boolean = sendPostRequest(
    url = SMS_PATTERN_URL,
    body = RequestSMS(
        code = "yrcb63deur93gml",
        recipient = recipient,
        sender = "+983000505",
        variable = RequestSMS.Variable(
            hash = hash, otp = otp
        )
    ),
    headers = mapOf(
        "apikey" to SMS_PANEL_API
    )
)

fun sendStateMessage(recipient: String, msg: String): Boolean = sendPostRequest(
    url = SMS_NORMAL_URL,
    body = NormalSMS(
        recipient = listOf(recipient),
        sender = "+983000505",
        message = msg
    ),
    headers = mapOf(
        "apikey" to SMS_PANEL_API
    )
)

@Serializable
data class TokenClaim(
    val flavor: Flavor,
    val deviceBrand: String? = null,
    val deviceModel: String? = null,
    val userAgent: String? = null,
    val deviceApi: Int? = null,
    val userID: Long,
)

enum class Flavor {
    ADMIN, PERSIAN, NATIVE;

    val isPersian
        get() = this == PERSIAN
}

