package example.com.routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Route.uploadRoutes() {
    route("/upload") {
        post {
            val multipart = call.receiveMultipart()
            var fileDescription: String? = null
            var fileBytes: ByteArray?
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val fileName = part.originalFileName ?: "unknown.jpg"
                        fileBytes = part
                            .streamProvider()
                            .readBytes()

                        // Check the file size (in bytes)
                        if (fileBytes != null && fileBytes!!.size > 1_000_000) {
                            call.respond(HttpStatusCode.PayloadTooLarge, "File size exceeds 1 MB limit.")
                            return@forEachPart
                        }

                        // Save the file or process it
                        val file = File("uploads/$fileName")
                        file.writeBytes(fileBytes!!)
                    }

                    else -> part.dispose()
                }
            }

            call.respondText("File uploaded successfully", status = HttpStatusCode.OK)
        }
    }
}