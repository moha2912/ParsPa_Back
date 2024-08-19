package example.com.plugins

import example.com.data.schema.*
import example.com.routes.orderRoutes
import example.com.routes.uploadRoutes
import example.com.routes.userRoutes
import example.com.routes.versionRoutes
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

@Serializable
data class OTPRequest(
    val email: String,
    val code: Int? = null,
)

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

        route("/api") {
            versionRoutes(versionService)
            userRoutes(userService, otpService)
            authenticate {
                get("/images/{id}") {
                    val imageFile = File("c:\\a.jpg")
                    if (imageFile.exists()) {
                        call.respondFile(imageFile)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Image not found")
                    }
                }
                orderRoutes(userService, orderService)
                uploadRoutes()
            }
        }
    }
}