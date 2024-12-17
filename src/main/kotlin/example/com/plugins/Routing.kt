package example.com.plugins

import example.com.data.model.res.BaseResponse
import example.com.data.schema.*
import example.com.env
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
        val orderStatesService = OrderStateService(database)
        val financeStatesService = FinanceStateService(database)
        val introService = IntroductionStateService(database)

        runBlocking {
            orderStatesService.addStates()
            financeStatesService.addStates()
        }

        val userService = UserService(database)
        val adminUserService = AdminUserService(database)
        val otpService = OTPService(database)
        val orderService = OrderService(database)
        val financialService = FinancialService(database)
        val versionService = VersionsService(database)
        val pricesService = PricesService(database)

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
                    msg = "ParsPa-AI API v1.7.3 ".plus("($env)")
                )
            )
        }

        paymentRoutes(userService, orderService, financialService, pricesService)
        versionRoutes(versionService)
        userRoutes(userService, otpService, introService)
        adminRoutes(adminUserService, versionService, userService, orderService, financialService)
        authenticate {
            orderRoutes(userService, orderService, financialService, pricesService)
            uploadRoutes()
        }
    }
}