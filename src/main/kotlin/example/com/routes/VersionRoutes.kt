package example.com.routes

import example.com.data.model.res.BaseResponse
import example.com.data.schema.ExposedVersion
import example.com.data.schema.VersionsService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class RequestVersion(
    val name: String,
    val versionCode: Int,
)

fun Route.versionRoutes(versionsService: VersionsService) {
    route("/version") {
        post("/apk") {
            val versionRequest = call.receive<RequestVersion>()
            val version = versionsService.read(versionRequest.name)
            if (version == null || version.versionCode <= versionRequest.versionCode) {
                call.respond(
                    message = BaseResponse("Ok."),
                )
            } else {
                call.respond(
                    message = version,
                    status = HttpStatusCode.NotAcceptable
                )
            }
        }
    }
}