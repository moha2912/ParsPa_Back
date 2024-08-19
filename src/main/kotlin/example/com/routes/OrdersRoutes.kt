package example.com.routes

import example.com.data.model.res.BaseResponse
import example.com.data.model.res.OrdersResponse
import example.com.data.schema.ExposedOrder
import example.com.data.schema.OrderService
import example.com.data.schema.UserService
import example.com.plugins.getIdFromToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class InsoleRequest(
    val orderID: Long,
    val address: String,
    val phone: String,
    val count: Int,
)

@Serializable
data class ReadOrderRequest(
    val orderID: Long,
)

fun Route.orderRoutes(userService: UserService, orderService: OrderService) {
    route("/orders") {
        get {
            val id = getIdFromToken()
            val orders = orderService.read(id)
            call.respond(
                message = OrdersResponse(
                    msg = "Ok.",
                    orders = orders
                ),
            )
        }
        get("/unread") {
            val id = getIdFromToken()
            val orders = orderService.readUnread(id)
            call.respond(
                message = OrdersResponse(
                    msg = "Ok.",
                    orders = orders
                ),
            )
        }
        post("/new") {
            val id = getIdFromToken()
            val order = call.receive<ExposedOrder>()
            if (order.feetLength !in 15f..40f) {
                call.respond(
                    message = BaseResponse(
                        msg = "Feet length must between 10-50 cm.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            val images = order.images
            if (images.hasBlank) {
                call.respond(
                    message = BaseResponse(
                        msg = "(${images.whichIsBlank}) image ids are blank.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (images.hasNotExists(id)) {
                call.respond(
                    message = BaseResponse(
                        msg = "(${images.whichIsNotExists(id)}) image ids are blank.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            orderService.create(id, order)
            call.respond(
                message = BaseResponse(
                    msg = "Ok.",
                ),
            )
        }
        post("/read") {
            val order = call.receive<ReadOrderRequest>()
            orderService.readOrder(order.orderID)
            call.respond(
                message = BaseResponse(
                    msg = "Ok.",
                ),
            )
        }
        post("/insole") {
            val id = getIdFromToken()
            if (userService.isNotFilled(id)) {
                call.respond(
                    message = BaseResponse(
                        msg = "You must complete profile first.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }

            val order = call.receive<InsoleRequest>()
            if (order.address.isBlank()) {
                call.respond(
                    message = BaseResponse(
                        msg = "Address field cannot be empty.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (order.phone.length < 7) {
                call.respond(
                    message = BaseResponse(
                        msg = "Phone field must be greater than 7.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (order.count < 1) {
                call.respond(
                    message = BaseResponse(
                        msg = "Count must be greater than 0",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (orderService.isNotExists(order.orderID)) {
                call.respond(
                    message = BaseResponse(
                        msg = "The order is not exists",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }

            orderService.addInsole(id, order)
            call.respond(
                message = BaseResponse(
                    msg = "Ok.",
                ),
            )
        }
        // -----------------------------------------------------------------------
        // todo
        /*get("/admin") {
            val id = getIdFromToken()
            val orders = orderService.readUnread(id)
            call.respond(
                message = OrdersResponse(
                    msg = "Ok.",
                    orders = orders
                ),
            )
        }
        get("/changeState") {
            val id = getIdFromToken()
            val orders = orderService.readUnread(id)
            call.respond(
                message = OrdersResponse(
                    msg = "Ok.",
                    orders = orders
                ),
            )
        }*/
    }
}