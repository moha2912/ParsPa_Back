package example.com.routes

import example.com.DEBUG_DOMAIN
import example.com.data.model.OrderState
import example.com.data.model.res.AdminUserResponse
import example.com.data.model.res.BaseResponse
import example.com.data.model.res.FinancesResponse
import example.com.data.model.res.OrdersResponse
import example.com.data.schema.*
import example.com.isDebug
import example.com.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.rmi.ServerException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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

val activeTasks = ConcurrentHashMap<String, String>()

fun Route.adminRoutes(
    adminService: AdminUserService,
    versionsService: VersionsService,
    userService: UserService,
    orderService: OrderService,
    financialService: FinancialService,
) {
    route("/admin") {
        get("/updateArticle") {
            sendExecuteCommand("update_article")//update_article
        }
        get("/updatePwa") {
            sendExecuteCommand("update_pwa")
        }
        get("/updateLanding") {
            sendExecuteCommand("update_landing")
        }

        // -----------------------------------------------------------------------

        get("/checkStatus") {
            val taskId = call.parameters["taskId"] ?: return@get call.respondText("Invalid task ID")
            val outputFile = activeTasks[taskId] ?: return@get call.respondText("Task not found")

            val file = File(outputFile)
            if (!file.exists()) {
                call.respondText("No output yet...")
                return@get
            }

            val output = file.readText()
            call.respondText(output)
            if (output.contains("[Process completed]")) {
                activeTasks.remove(taskId)
            }
        }

        get("/resetBot") {
            if (isDebug) {
                executeCommand("/bin/bash /root/reset_proxy.sh") {
                    TelegramBot.sendBotMessage()
                    call.respondText(
                        """
                    <html>
                        <body>
                            <h1>Resetting bot...</h1>
                        </body>
                    </html>
                """, ContentType.Text.Html
                    )
                }
                return@get
            }
            if (!isDebug) {
                executeCommand("/bin/bash /root/reset_proxy.sh")
                delay(10000)
                TelegramBot.sendBotMessage()
                call.respondRedirect(DEBUG_DOMAIN.plus("admin/resetBot"))
            }
            TelegramBot.sendBotMessage()
            call.respondText(
                """
                    <html>
                        <body>
                            <h1>Resetting bot...</h1>
                        </body>
                    </html>
                """, ContentType.Text.Html
            )
        }

        // -----------------------------------------------------------------------

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
                if (!isDebug) {
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

suspend fun PipelineContext<Unit, ApplicationCall>.sendExecuteCommand(
    script: String,
) {
    val taskId = UUID
        .randomUUID()
        .toString()
    val outputFile = "/root/updateLogs/${script}_$taskId.log"
    val scriptToRun = "/root/$script.sh"

    activeTasks[taskId] = outputFile

    withContext(Dispatchers.IO) {
        ProcessBuilder("/bin/bash", "/root/runner.sh", outputFile, scriptToRun).start()
    }

    call.respondText(
        """
                        <html>
                            <body>
                                <h1 id="header">Starting command...</h1>
                                <pre id="output"></pre>
                                <script>
                                    var taskId = "$taskId";
                                    document.title = "Processing...";
                                    function checkStatus() {
                                        fetch("/admin/checkStatus?taskId=" + taskId)
                                            .then(response => response.text())
                                            .then(data => {
                                                document.getElementById('output').innerHTML = data;
                                                if (data.includes("[Process completed]")) {
                                                    document.title = "Finished";
                                                    document.getElementById('header').innerHTML = "Finished!";
                                                } else {
                                                    setTimeout(checkStatus, 5000);
                                                }
                                            });
                                    }
                                    checkStatus();
                                </script>
                            </body>
                        </html>
                    """, ContentType.Text.Html
    )
}

suspend fun executeCommand(command: String, onLineRead: suspend (String) -> Unit = {}) {
    withContext(Dispatchers.IO) {
        val process = ProcessBuilder(command.split(" ")).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        try {
            var line: String?
            while (reader
                    .readLine()
                    .also { line = it } != null
            ) {
                onLineRead(line!!)
            }
        } finally {
            reader.close()
            process.waitFor()
            onLineRead("[Process completed with exit code ${process.exitValue()}]")
        }
    }
}