package example.com.routes

import example.com.USERS_FOLDER
import example.com.data.model.exception.DetailedException
import example.com.data.model.res.BaseResponse
import example.com.data.model.res.OTPResponse
import example.com.data.model.res.UserResponse
import example.com.data.schema.ExposedUser
import example.com.data.schema.OTPService
import example.com.data.schema.UserService
import example.com.plugins.createToken
import example.com.plugins.getFlavorHeader
import example.com.plugins.getIdFromToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.TimeUnit

fun getUserFolder(id: Long) = USERS_FOLDER.plus(
    id
        .toString()
        .plus("/")
)

fun getUserAvatar(id: Long) = getUserFolder(id).plus("avatar")
fun getUserImages(id: Long) = getUserFolder(id).plus("img/")
fun getUserVideos(id: Long) = getUserFolder(id).plus("vid/")

@Serializable
data class OTPRequest(
    val field: String,
    val code: Int? = null,
)

val smsRateLimit = mutableMapOf<String, Long>()
val OTP_TIME = TimeUnit.MINUTES.toMillis(2)
val SMS_OTP_TIME = TimeUnit.MINUTES.toMillis(5)
val RATE_LIMIT_DURATION = TimeUnit.HOURS.toMillis(6)
const val MAX_ATTEMPTS = 2

fun Route.userRoutes(userService: UserService, otpService: OTPService) {
    route("/user") {
        post("/request") {
            val otpRequest = call.receive<OTPRequest>()
            val flavor = getFlavorHeader() // todo: change path for persian-native
            val time = if (flavor.isPersian) SMS_OTP_TIME else OTP_TIME
            val field = otpRequest.field

            if (field.isBlank()) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = BaseResponse(
                        msg = "Empty field.",
                    ),
                )
                return@post
            }

            val otpRow = otpService.read(field)
            if (otpRow != null) {
                call.respond(
                    OTPResponse(
                        msg = "The code was already sent.",
                        remaining = otpRow.created
                            .plus(time)
                            .minus(System.currentTimeMillis())
                    )
                )
                return@post
            }

            if (flavor.isPersian) {
                // todo send message
                if (field.any { !it.isDigit() }) {
                    call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = BaseResponse(
                            msg = "Wrong phone format",
                        )
                    )
                    return@post
                }
                val ip = call.request.local.remoteAddress
                val requestTimes = smsRateLimit[ip] ?: 0
                if (requestTimes >= MAX_ATTEMPTS) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        OTPResponse(
                            msg = "You have exceeded rate limit and blocked for 6 hours.",
                            remaining = -1
                        )
                    )
                    return@post
                }
                smsRateLimit[field] = requestTimes + 1
                otpService.create(field)
            } else {
                otpService.create(field) // todo phone
            }
            call.respond(
                OTPResponse(
                    msg = "The code has sent successfully.",
                    remaining = time
                )
            )
        }
        post("/login") {
            val flavor = getFlavorHeader()
            val otpRequest = call.receive<OTPRequest>()
            otpRequest.code ?: throw NullPointerException()
            val field = otpRequest.field
            val otp = otpRequest.code

            val otpRow = otpService.read(field, otp)
            if (otpRow == null) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = BaseResponse(
                        msg = "The code has expired or is wrong.",
                    ),
                )
                return@post
            }

            otpService.delete(field)
            val exposedUser = if (flavor.isPersian)
                userService.readPhone(field)
            else
                userService.readEmail(field)

            val id = exposedUser?.id ?: if (flavor.isPersian)
                userService.createPhone(field)
            else
                userService.create(field)

            call.respond(
                UserResponse(
                    msg = "Successfully logged in.",
                    token = createToken(id),
                    user = exposedUser?.also {
                        it.id = null
                    }
                )
            )
        }
        authenticate {
            profileRoutes(userService)
        }
        // -----------------------------------------------------------------------
        //todo admin
    }
}

fun Route.profileRoutes(userService: UserService) {
    route("/avatar") {
        get {
            val id = getIdFromToken()
            val imageFile = File(getUserAvatar(id))
            if (imageFile.exists()) {
                call.respondFile(imageFile)
            } else {
                call.respond(HttpStatusCode.NotFound, "Empty avatar")
            }
        }
        post {
            val id = getIdFromToken()
            val filePath = getUserAvatar(id)
            receiveFile(
                fileName = filePath,
            )
        }
    }
    route("/profile") {
        get {
            val id = getIdFromToken()
            val exposedUser = userService.readID(id)
            call.respond(
                UserResponse(
                    msg = "Ok.",
                    user = exposedUser.also {
                        it?.id = null
                    }
                )
            )
        }
        put {
            val id = getIdFromToken()
            val flavor = getFlavorHeader()
            val databaseUser = userService.readID(id)
            val userParams = call.receive<ExposedUser>()
            if (flavor.isPersian) {
                userParams.phone = null
                if (!databaseUser?.email.isNullOrBlank()) {
                    userParams.email = null
                }
            } else {
                userParams.email = null
                if (!databaseUser?.phone.isNullOrEmpty()) {
                    userParams.phone = null
                }
            }
            userParams.name?.let {
                if (it.isBlank()) {
                    throw DetailedException("Full name must not be empty")
                }
            }
            userParams.gender?.let {
                if (it.toInt() !in 0..1) {
                    throw DetailedException("Gender must be 0 or 1")
                }
            }
            userParams.birthday?.let {
                if (!it.contains("/") || it.count { it == '/' } != 2) {
                    throw DetailedException("Birthday should sent like 30/12/1990")
                }
            }
            userParams.phone?.let {
                if (it.length < 11) {
                    throw DetailedException("Phone number format is not supported")
                }
            }
            userParams.address?.let {
                if (it.length < 10) {
                    throw DetailedException("Address length must greater than 10")
                }
            }
            userService.update(id, userParams)
            val user = userService.readID(id)
            call.respond(
                UserResponse(
                    msg = "Ok.",
                    user = user
                )
            )
        }
    }
}