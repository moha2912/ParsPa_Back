package example.com.routes

import example.com.data.model.OrderState
import example.com.data.model.res.BaseResponse
import example.com.data.model.res.OrderResponse
import example.com.data.model.res.OrdersResponse
import example.com.data.schema.ExposedOrder
import example.com.data.schema.OrderService
import example.com.data.schema.UserService
import example.com.plugins.getIdFromToken
import example.com.plugins.getPathParameter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class InsoleRequest(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val orderID: Long? = null,
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
            val orders = orderService.readOrders(id)
            call.respond(
                message = OrdersResponse(
                    msg = "Ok.",
                    orders = orders
                ),
            )
        }
        get("/unread") {
            val id = getIdFromToken()
            val orders = orderService.readUnreadOrders(id)
            call.respond(
                message = OrdersResponse(
                    msg = "Ok.",
                    orders = orders
                ),
            )
        }
        get("/{id}") {
            val id = getIdFromToken()
            val pathID = getPathParameter("id")?.toLong() ?: -1
            val order = orderService.readOrder(id, pathID) ?: throw NotFoundException()
            call.respond(
                message = OrderResponse(
                    msg = "Ok.",
                    order = order
                ),
            )
        }
        post("/new") {
            val id = getIdFromToken()
            val requestOrder = call.receive<ExposedOrder>()
            if (requestOrder.feetLength !in 15f..40f) {
                call.respond(
                    message = BaseResponse(
                        msg = "Feet length must between 10-50 cm.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (requestOrder.weight !in 19f..121f) {
                call.respond(
                    message = BaseResponse(
                        msg = "Weight must between 20-120 kg.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (requestOrder.feetSize !in 30..50) {
                call.respond(
                    message = BaseResponse(
                        msg = "Feet size must between 30-50.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            val images = requestOrder.images
            if (!requestOrder.isNotBlank) {
                call.respond(
                    message = BaseResponse(
                        msg = "one or few image ids are blank.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (!requestOrder.isAllExists(id)) {
                call.respond(
                    message = BaseResponse(
                        msg = "One or few image ids are not found.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (requestOrder.orderID != null) {
                orderService.readOrder(id, requestOrder.orderID) ?: throw NotFoundException()
                orderService.update(requestOrder.orderID, requestOrder)
            } else {
                orderService.create(id, requestOrder)
            }
            call.respond(
                message = BaseResponse(
                    msg = "Ok.",
                ),
            )
        }
        post("/read") {
            val id = getIdFromToken()
            val order = call.receive<ReadOrderRequest>()
            orderService.setReadOrder(
                userID = id,
                id = order.orderID,
            )
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
            val existsOrder = orderService.readOrder(id, order.orderID) ?: throw NotFoundException()
            if (existsOrder.state != OrderState.DOCTOR_RESPONSE) {
                call.respond(
                    message = BaseResponse(
                        msg = "The order is not ready for request insole",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }

            orderService.addInsole(order)
            call.respond(
                message = BaseResponse(
                    msg = "Ok.",
                ),
            )
        }
    }
}