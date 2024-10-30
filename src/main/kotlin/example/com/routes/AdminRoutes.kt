package example.com.routes

import example.com.data.model.OrderState
import example.com.data.model.res.AdminUserResponse
import example.com.data.model.res.BaseResponse
import example.com.data.model.res.FinancesResponse
import example.com.data.model.res.OrdersResponse
import example.com.data.schema.*
import example.com.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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

@Serializable
data class UploadApp(
    val appName: String,
    val fileName: String,
    val version: String,
    val versionCode: Int,
    val lastChanges: String,
)

fun Route.adminRoutes(
    adminService: AdminUserService,
    versionsService: VersionsService,
    userService: UserService,
    orderService: OrderService,
    financialService: FinancialService,
) {
    route("/admin") {
        get("/updatePwa") {
            call.respondText(
                """
                    <html>
                        <body>
                            <p>Update in <span id="timer">60</span> seconds...</p>
                            <script>
                                var timeLeft = 60;
                                var timerElement = document.getElementById('timer');
                                
                                // هر ثانیه تایمر رو آپدیت می‌کنه
                                var countdown = setInterval(function() {
                                    if (timeLeft <= 0) {
                                        clearInterval(countdown);
                                    } else {
                                        timeLeft--;
                                        timerElement.innerHTML = timeLeft;
                                    }
                                }, 1000); // هر 1000 میلی‌ثانیه (1 ثانیه) اجرا میشه
                            </script>
                        </body>
                    </html>
                """, ContentType.Text.Html
            )
            executeCommand("/bin/bash /root/update_pwa.sh")
        }
        get("/updateLanding") {
            call.respondText(
                """
                    <html>
                        <body>
                            <p>Update in <span id="timer">60</span> seconds...</p>
                            <script>
                                var timeLeft = 60;
                                var timerElement = document.getElementById('timer');
                                
                                // هر ثانیه تایمر رو آپدیت می‌کنه
                                var countdown = setInterval(function() {
                                    if (timeLeft <= 0) {
                                        clearInterval(countdown);
                                    } else {
                                        timeLeft--;
                                        timerElement.innerHTML = timeLeft;
                                    }
                                }, 1000); // هر 1000 میلی‌ثانیه (1 ثانیه) اجرا میشه
                            </script>
                        </body>
                    </html>
                """, ContentType.Text.Html
            )
            executeCommand("/bin/bash /root/update_landing.sh")
        }
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
                val start = getQueryParameter("start")?.toLongOrNull() ?: 0
                val end = getQueryParameter("end")?.toLongOrNull() ?: 0
                call.respond(
                    message = OrdersResponse(
                        msg = "Ok.",
                        orders = orderService.getAllOrders(filter, start, end)
                    ),
                )
            }
            get("/finances") {
                checkAdminUser(adminService)
                val start = getQueryParameter("start")?.toLongOrNull() ?: 0
                val end = getQueryParameter("end")?.toLongOrNull() ?: 0
                val finances = financialService.getAllFinances(start, end)
                call.respond(
                    message = FinancesResponse(
                        msg = "Ok.",
                        amount = finances.sumOf { it.zibal?.amount ?: 0 },
                        count = finances.sumOf { it.insole.count },
                        finances = finances
                    ),
                )
            }
            post("/orders") {
                checkAdminUser(adminService)
                val order = call.receive<ChangeState>()
                orderService.updateState(order)
                orderService
                    .adminGetOrder(order.orderID)
                    ?.let { // todo use JOIN
                        userService
                            .readID(it.userID)
                            ?.let {
                                val phone = it.phone ?: return@let
                                val name = it.name
                                val message = "%s عزیز!\n%s\nweb.parspa-ai.ir".format(name, order.newState.msg)
                                sendStateMessage(recipient = phone, msg = message)
                            }
                    }
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
            post("/app") {
                checkAdminUser(adminService)
                receiveApp()?.let {
                    versionsService.update(it)
                    call.respond(
                        message = BaseResponse(
                            msg = "Ok.",
                        ),
                    )
                } ?: call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = BaseResponse(
                        msg = "Nothing was uploaded"
                    )
                )
            }
        }
    }
}

suspend fun executeCommand(command: String): String {
    return withContext(Dispatchers.IO) {
        val process = ProcessBuilder(command.split(" ")).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readText()
        reader.close()
        process.waitFor()
        output
    }
}