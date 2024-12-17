package example.com.plugins

import example.com.data.model.exception.AuthorizationException
import example.com.data.model.res.BaseResponse
import example.com.isDebug
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                message = BaseResponse("Not found"),
                status = status
            )
        }
        status(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden) { call, status ->
            call.respond(
                message = BaseResponse("You don't have permission"),
                status = status
            )
        }
        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            call.respond(
                message = BaseResponse("Wrong method"),
                status = status
            )
        }
        exception<Throwable> { call, cause ->
            when (cause) {
                is NotFoundException -> {
                    call.respond(
                        message = BaseResponse("Not found"),
                        status = HttpStatusCode.NotFound
                    )
                }
                is BadRequestException, is NullPointerException, is IllegalStateException -> {
                    val msg = buildString {
                        append("Bad request")
                        if (isDebug) {
                            appendLine(cause.stackTraceToString())
                        }
                    }
                    call.respond(
                        message = BaseResponse(msg),
                        status = HttpStatusCode.BadRequest
                    )
                }

                is AuthorizationException ->
                    call.respond(
                        message = BaseResponse("You don't have permission"),
                        status = HttpStatusCode.Unauthorized
                    )

                else ->
                    call.respond(
                        message = BaseResponse("Server error, try again later"),
                        status = HttpStatusCode.InternalServerError
                    )
            }
        }
    }
}