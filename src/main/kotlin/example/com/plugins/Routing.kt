package example.com.plugins

import example.com.data.schema.*
import example.com.routes.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

fun Application.configureRouting() {
    install(Resources)
    routing {
        val database = ParsPaDatabase.connectDatabase()
        val userService = UserService(database)
        val otpService = OTPService(database)
        val orderService = OrderService(database)
        val versionService = VersionsService(database)

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

        versionRoutes(versionService)
        userRoutes(userService, otpService)
        authenticate {
            orderRoutes(userService, orderService)
            uploadRoutes()
        }
    }
}