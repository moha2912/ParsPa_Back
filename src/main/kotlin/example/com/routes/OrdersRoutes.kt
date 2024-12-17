package example.com.routes

import example.com.ZIBAL_MERCHANT
import example.com.ZIBAL_START_URL
import example.com.data.model.OrderState
import example.com.data.model.exception.DetailedException
import example.com.data.model.res.BaseResponse
import example.com.data.model.res.OrderResponse
import example.com.data.model.res.OrdersResponse
import example.com.data.model.res.PaymentResponse
import example.com.data.schema.*
import example.com.plugins.TelegramBot
import example.com.plugins.createPayRequest
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
    val postID: Long = 9123456789, //todo
    val name: String = "کاربر عزیز", //todo
    val address: String,
    val phone: String,
    val count: Int,
    val platform: Short = 0,
)

@Serializable
data class ReadOrderRequest(
    val orderID: Long,
)

fun Route.orderRoutes(
    userService: UserService,
    orderService: OrderService,
    financialService: FinancialService,
    pricesService: PricesService,
) {
    route("/orders") {
        get {
            val id = getIdFromToken()
            val orders = orderService.getOrders(id)
            call.respond(
                message = OrdersResponse(
                    msg = "Ok.",
                    orders = orders
                ),
            )
        }
        get("/unread") {
            val id = getIdFromToken()
            val orders = orderService.getUnreadOrders(id)
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
            val order = orderService.getOrder(id, pathID) ?: throw NotFoundException()
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
            if (requestOrder.feetWidth !in 5f..15f) {
                call.respond(
                    message = BaseResponse(
                        msg = "Feet width must between 5-15 cm.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
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
            if (requestOrder.feetSize !in 30f..50f) {
                call.respond(
                    message = BaseResponse(
                        msg = "Feet size must between 30-50.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (requestOrder.feetSize.mod(0.5f) != 0f) {
                call.respond(
                    message = BaseResponse(
                        msg = "Feet size must be with 0.5 precision",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (requestOrder.gender !in 0..1) {
                call.respond(
                    message = BaseResponse(
                        msg = "Gender must be 0(Male) or 1(Female)",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (requestOrder.platform !in 0..1) {
                call.respond(
                    message = BaseResponse(
                        msg = "Platform must be 0(Application) or 1(WebApp)",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (requestOrder.age !in 1..100) {
                call.respond(
                    message = BaseResponse(
                        msg = "Age is not acceptable (1-100).",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
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
            val phone = userService.getPhone(id)
            var orderID: Long? = null
            if (requestOrder.orderID != null) {
                orderService.getOrder(id, requestOrder.orderID) ?: throw NotFoundException()
                orderService.update(requestOrder.orderID, requestOrder)
            } else {
                orderID = orderService.create(id, requestOrder)
            }
            call.respond(
                message = BaseResponse(
                    msg = "Ok.",
                ),
            )
            orderID?.let {
                TelegramBot.sendCreateOrder(it, phone, requestOrder.platform)
            }
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
            if (order.postID.toString().length != 10) {
                call.respond(
                    message = BaseResponse(
                        msg = "Post id length must be 10.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            if (order.name.isBlank()) {
                call.respond(
                    message = BaseResponse(
                        msg = "Name cannot be empty.",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
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
            if (order.platform !in 0..1) {
                call.respond(
                    message = BaseResponse(
                        msg = "Platform must be 0(Application) or 1(WebApp)",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }
            val existsOrder = orderService.getOrder(id, order.orderID) ?: throw NotFoundException()
            if (existsOrder.state != OrderState.DOCTOR_RESPONSE) {
                call.respond(
                    message = BaseResponse(
                        msg = "The order is not ready for request insole",
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }

            val user = userService.readID(id) ?: throw DetailedException("User unavailable")
            val price = pricesService.read("insole") ?: throw DetailedException("Server error!")
            val amount = order.count * price.price * 10L
            val request = createPayRequest(
                merchant = ZIBAL_MERCHANT,
                amount = amount,
                desc = "سفارش کفی اختصاصی ${user.name}",
                phone = user.phone ?: ""
            )
            request ?: throw DetailedException("Payment unavailable")
            if (request.result != 100) throw DetailedException("Payment unavailable")
            financialService.updateError(order.orderID)
            financialService.create(
                ExposedFinance(
                    date = System.currentTimeMillis(),
                    trackID = request.trackId,
                    userID = id,
                    orderID = order.orderID ?: -1,
                    platform = order.platform,
                    insole = order,
                )
            )

            call.respond(
                message = PaymentResponse(
                    link = ZIBAL_START_URL.plus(request.trackId),
                    amount = amount
                ),
            )
            // TODO: TelegramBot.sendCreatedPayment(user, order, amount)
        }
    }
}