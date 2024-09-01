package example.com.routes

import example.com.data.model.OrderState
import example.com.data.model.res.AdminUserResponse
import example.com.data.model.res.BaseResponse
import example.com.data.model.res.OrdersResponse
import example.com.data.schema.AdminUserService
import example.com.data.schema.OrderService
import example.com.data.schema.UserService
import example.com.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File
import java.rmi.ServerException

@Serializable
data class RequestLoginAdmin(
    val username: String,
    val password: String,
)

@Serializable
data class ChangeState(
    val orderID: Long,
    val newState: OrderState,
    val doctorResponse: String? = null,
    val resendPictures: List<String>? = null,
)

fun Route.adminRoutes(
    adminService: AdminUserService,
    userService: UserService,
    orderService: OrderService
) {
    route("/admin") {
        post("/login") {
            val user = call.receive<RequestLoginAdmin>()
            val adminUser =
                adminService.readUser(user.username, user.password) ?: throw BadRequestException("No user found")

            val id = adminUser.id ?: throw ServerException("")
            call.respond(
                AdminUserResponse(
                    msg = "Successfully logged in.",
                    token = createToken(
                        TokenClaim(
                            flavor = Flavor.ADMIN,
                            userID = id
                        )
                    ),
                    user = adminUser.copy(
                        id = null,
                        password = null
                    )
                )
            )
        }
        authenticate {
            post("/check") {
                checkAdminUser(adminService)
                call.respond(
                    message = BaseResponse(
                        msg = "Ok.",
                    ),
                )
            }
            get("/orders") {
                checkAdminUser(adminService)
                val filter = getQueryParameter("filter")
                call.respond(
                    message = OrdersResponse(
                        msg = "Ok.",
                        orders = orderService.readAllOrders(filter)
                    ),
                )
            }
            post("/orders") {
                checkAdminUser(adminService)
                val order = call.receive<ChangeState>()
                orderService.updateState(order)
                call.respond(
                    message = BaseResponse(
                        msg = "Ok.",
                    ),
                )
            }
            post("/orders/read/{id}") {
                checkAdminUser(adminService)
                val id = getPathParameter("id")?.toLong() ?: -1
                orderService.setAdminReadOrder(id)
                call.respond(
                    message = BaseResponse(
                        msg = "Ok.",
                    ),
                )
            }
            get("/pic/{id}") {
                checkAdminUser(adminService)
                val id = getHeader("UserID")?.toLongOrNull() ?: throw NotFoundException()
                val imageID = getPathParameter("id")
                if (!imageID.isNullOrBlank()) {
                    val imageFile = File(getUserImages(id).plus(imageID))
                    if (imageFile.exists()) {
                        call.respondFile(imageFile)
                        return@get
                    }
                }
                call.respond(HttpStatusCode.NotFound, "Image not found")
            }
        }
    }
}