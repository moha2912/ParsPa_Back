package example.com.routes

import example.com.USERS_FOLDER
import example.com.data.model.res.BaseResponse
import example.com.plugins.getIdFromToken
import example.com.plugins.getPathParameter
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class RequestImageDelete(
    val imageID: String
)

fun Route.uploadRoutes() {
    route("/upload") {
        post("/pic") {
            val id = getIdFromToken()
            val fileName = "img_${System.currentTimeMillis()}"
            val filePath = getUserImages(id).plus(fileName)
            receiveFile(
                fileName = filePath,
                size = 1000000,
            )
        }
        get("/pic/{id}") {
            val id = getIdFromToken()
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
        delete("/pic") {
            val id = getIdFromToken()
            val request = call.receive<RequestImageDelete>()
            val fileName = request.imageID
            val filePath = getUserImages(id).plus(fileName)
            File(filePath).delete()
            call.respond(
                BaseResponse(
                    msg = "Ok."
                )
            )
        }
        post("/vid") {
            val id = getIdFromToken()
            val fileName = "vid_${System.currentTimeMillis()}"
            val filePath = getUserVideos(id).plus(fileName)
            receiveFile(
                fileName = filePath,
                size = 1000000,
            )
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.receiveFile(
    fileName: String,
    size: Long = 1000000,
) {
    val multipart = call.receiveMultipart()
    var fileBytes: ByteArray
    multipart
        .readPart()
        ?.let { part ->
            when (part) {
                is PartData.FileItem -> {
                    fileBytes = part
                        .streamProvider()
                        .readBytes()
                    if (fileBytes.size > size) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            message = BaseResponse(
                                msg = "File size exceeds limit"
                            )
                        )
                        return@let
                    }
                    val folder = File(fileName)
                    if (!folder.exists()) {
                        folder.parentFile.mkdirs()
                    } else {
                        folder.delete()
                    }
                    if (folder.createNewFile()) {
                        folder.writeBytes(fileBytes)
                    }
                    call.respond(
                        message = BaseResponse(
                            msg = folder.name
                        )
                    )
                }

                else -> part.dispose()
            }
        } ?: call.respond(
        status = HttpStatusCode.BadRequest,
        message = BaseResponse(
            msg = "Nothing was uploaded"
        )
    )
}
