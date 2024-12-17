package example.com.routes

import example.com.plugins.TelegramBot
import example.com.DL_HOST
import example.com.DL_PATH
import example.com.data.model.res.BaseResponse
import example.com.data.schema.ExposedVersion
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
        delete("/pic/{id}") {
            val id = getIdFromToken()
            val imageID = getPathParameter("id")
            val filePath = getUserImages(id).plus(imageID)
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
    onResponse: (suspend () -> Unit)? = null
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
                    onResponse?.invoke() ?: call.respond(
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


suspend fun PipelineContext<Unit, ApplicationCall>.receiveApp(): ExposedVersion? {
    val multipart = call.receiveMultipart()
    var fileBytes: ByteArray

    var appName = ""
    var fileName = ""
    var version = ""
    var versionCode: Int = -1
    var lastChanges = ""
    var mandatory = false
    var saved = false

    multipart
        .forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    when (part.name) {
                        "appName" -> appName = part.value
                        "fileName" -> fileName = part.value
                        "version" -> version = part.value
                        "versionCode" -> versionCode = part.value.toIntOrNull() ?: -1
                        "lastChanges" -> lastChanges = part.value
                        "mandatory" -> mandatory = part.value.toBoolean()
                    }
                }

                is PartData.FileItem -> {
                    try {
                        fileBytes = part
                            .streamProvider()
                            .readBytes()
                        val file = File(DL_PATH.plus(fileName))
                        if (!file.exists()) {
                            try {
                                file.parentFile.mkdirs()
                            } catch (_: Exception) {
                            }
                        } else {
                            file.delete()
                        }
                        if (file.createNewFile()) {
                            file.writeBytes(fileBytes)
                        }
                        saved = true
                    } catch (e: Exception) {
                        TelegramBot.sendError(e)
                    }
                }

                else -> part.dispose()
            }
        }

    return if (saved) ExposedVersion(
        name = appName,
        updateUrl = DL_HOST.plus(fileName),
        mandatory = mandatory,
        version = version,
        versionCode = versionCode,
        lastChanges = lastChanges
    ) else null
}
