package example.com.routes

import app.bot.TelegramBot
import example.com.*
import example.com.data.model.res.PaymentResultResponse
import example.com.data.model.res.PaymentReviewResponse
import example.com.data.model.res.PriceResponse
import example.com.data.schema.FinanceState
import example.com.data.schema.FinancialService
import example.com.data.schema.OrderService
import example.com.data.schema.UserService
import example.com.plugins.getIdFromToken
import example.com.plugins.getPathParameter
import example.com.plugins.getQueryParameter
import example.com.plugins.verifyPayment
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Route.paymentRoutes(
    userService: UserService,
    orderService: OrderService,
    financialService: FinancialService
) {
    route("/$PAYMENT_ROUTE") {
        get {
            val trackID = getQueryParameter("trackId")?.toLong() ?: -1
            val verify = verifyPayment(trackID = trackID, merchant = ZIBAL_MERCHANT)
            if (verify != null) {
                println()
                println("#PAYMENT_VERIFY")
                println(Json.encodeToString(verify))
                println()
                val finance = financialService.readID(trackID) ?: throw Exception()
                if (verify.result == 100) {
                    financialService.update(finance.id, trackID, FinanceState.SUCCESS, verify)
                    orderService.addInsole(finance.insole)
                    call.respondRedirect(DEEPLINK_SUCCESS)
                    TelegramBot.sendVerifiedPayment(verify)
                    TelegramBot.sendInsoleOrder(finance.insole)
                    return@get
                } else if (verify.result == 202) {
                    financialService.update(finance.id, trackID, FinanceState.ERROR, verify)
                }
            }
            call.respondRedirect(DEEPLINK_ERROR)
        }
        authenticate {
            get("/price") {
                // todo create database for prices
                call.respond(
                    message = PriceResponse(
                        price = INSOLE_PRICE,
                        priceFormatted = INSOLE_PRICE_FORMATTED
                    )
                )
            }
            get("/check/{id}") {
                val id = getIdFromToken()
                val orderID = getPathParameter("id")?.toLongOrNull() ?: -1
                val order = orderService.getOrder(id, orderID)
                val finance = financialService.readOrderID(orderID, id)
                call.respond(
                    message = PaymentResultResponse(
                        msg = when {
                            order == null || finance == null -> FinanceState.NO_ORDER
                            else -> finance.status
                        },
                        successDate = finance?.successDate,
                        zibal = finance?.zibal
                    ),
                )
            }
            get("/{id}") {
                val id = getIdFromToken()
                val orderID = getPathParameter("id")?.toLongOrNull() ?: throw NotFoundException()
                val finance = financialService.readOrderID(orderID, FinanceState.SUCCESS) ?: throw NotFoundException()
                call.respond(
                    message = PaymentReviewResponse(
                        date = finance.successDate ?: -1,
                        insole = finance.insole,
                        card = finance.zibal?.cardNumber ?: "",
                        refNum = finance.zibal?.refNumber ?: -1,
                        total = (finance.zibal?.amount ?: 10) / 10
                    ),
                )
            }
        }
    }
}