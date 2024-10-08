package example.com.plugins

import example.com.data.model.res.BaseResponse
import example.com.data.schema.*
import example.com.routes.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

fun Application.configureRouting() {
    install(Resources)
    routing {
        val database = ParsPaDatabase.connectDatabase()
        val userService = UserService(database)
        val adminUserService = AdminUserService(database)
        val otpService = OTPService(database)
        val orderService = OrderService(database)
        val financialService = FinancialService(database)
        val orderStatesService = OrderStateService(database)
        val financeStatesService = FinanceStateService(database)
        val versionService = VersionsService(database)

        runBlocking {
            orderStatesService.addStates()
            financeStatesService.addStates()
        }
        /*intercept(Plugins) {
            versionService.create(
                ExposedVersion(
                    name = "apk",
                    version = "1.0",
                    versionCode = 1,
                    lastChanges = "Hello changes",
                    updateUrl = "link"
                )
            )
        }*/
        get {
            call.respond(
                status = HttpStatusCode.OK,
                message = BaseResponse(
                    msg = "ParsPa-AI API v1.3"
                )
            )
        }

        paymentRoutes(userService, orderService, financialService)
        versionRoutes(versionService)
        userRoutes(userService, otpService)
        adminRoutes(adminUserService, versionService, userService, orderService)
        authenticate {
            orderRoutes(userService, orderService, financialService)
            uploadRoutes()
        }
    }
}