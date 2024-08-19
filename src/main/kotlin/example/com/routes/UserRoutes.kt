package example.com.routes

import example.com.data.model.exception.DetailedException
import example.com.data.model.res.BaseResponse
import example.com.data.model.res.UserResponse
import example.com.data.model.res.OTPResponse
import example.com.data.schema.*
import example.com.plugins.createToken
import example.com.plugins.getIdFromToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class OTPRequest(
    val email: String,
    val code: Int? = null,
)

fun Route.userRoutes(userService: UserService, otpService: OTPService) {
    route("/user") {
        post("/request") {
            val otpRequest = call.receive<OTPRequest>()
            val email = otpRequest.email

            val otpRow = otpService.read(email)
            if (otpRow != null) {
                call.respond(
                    OTPResponse(
                        msg = "The code was already sent.",
                        remaining = otpRow.created
                            .plus(OTP_TIME)
                            .minus(System.currentTimeMillis())
                    )
                )
                return@post
            }

            //todo send email
            otpService.create(email)
            call.respond(
                OTPResponse(
                    msg = "The code has sent successfully.",
                    remaining = OTP_TIME
                )
            )
        }
        post("/login") {
            val otpRequest = call.receive<OTPRequest>()
            otpRequest.code ?: throw NullPointerException()
            val email = otpRequest.email
            val otp = otpRequest.code

            val otpRow = otpService.read(email, otp)
            if (otpRow == null) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = BaseResponse(
                        msg = "The code has expired or is wrong.",
                    ),
                )
                return@post
            }

            otpService.delete(email)
            val exposedUser = userService.read(email)
            val id = exposedUser?.id ?: userService.create(ExposedUser(email = email))
            call.respond(
                UserResponse(
                    msg = "Successfully logged in.",
                    token = createToken(id),
                    user = exposedUser?.also {
                        it.id = null
                        it.email = null
                    }
                )
            )
        }
        authenticate {
            route("/profile") {
                get {
                    val id = getIdFromToken()
                    val exposedUser = userService.read(id)
                    call.respond(
                        UserResponse(
                            msg = "Ok.",
                            user = exposedUser
                        )
                    )
                }
                put {
                    val id = getIdFromToken()
                    val userParams = call.receive<ExposedUser>()
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
                        if (it.toString().length < 7) {
                            throw DetailedException("Phone number format is not supported")
                        }
                    }
                    userParams.address?.let {
                        if (it.length < 10) {
                            throw DetailedException("Address length must greater than 10")
                        }
                    }
                    userService.update(id, userParams)
                    val user = userService.read(id)
                    call.respond(
                        UserResponse(
                            msg = "Ok.",
                            user = user
                        )
                    )
                }
            }
        }
        // -----------------------------------------------------------------------
        //todo admin
    }
}