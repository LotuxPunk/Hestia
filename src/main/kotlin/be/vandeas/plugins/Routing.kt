package be.vandeas.plugins

import be.vandeas.domain.input.DirectoryDeleteOptions
import be.vandeas.domain.input.FileCreationOptions
import be.vandeas.domain.input.FileDeleteOptions
import be.vandeas.domain.input.FileReadOptions
import be.vandeas.domain.output.DirectoryDeleteResult
import be.vandeas.domain.output.FileCreationResult
import be.vandeas.domain.output.FileDeleteResult
import be.vandeas.domain.output.FileReadResult
import be.vandeas.logic.FileLogic
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val fileLogic by inject<FileLogic>()

    routing {
        route("/v1") {
            get {
                val options: FileReadOptions = call.receive()
                val accept = call.request.accept()

                when (val result = fileLogic.readFile(options)) {
                    is FileReadResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                    is FileReadResult.NotFound -> call.respond(HttpStatusCode.NotFound, mapOf("path" to options.path, "fileName" to options.fileName))
                    is FileReadResult.Success -> when(accept) {
                        ContentType.Application.Json.contentType -> call.respond(HttpStatusCode.OK, mapOf("content" to result.data.encodeBase64(), "fileName" to options.fileName))
                        ContentType.Application.OctetStream.contentType -> call.respondBytes(result.data)
                        else -> call.respond(HttpStatusCode.NotAcceptable, "Accept header must be application/json or application/octet-stream")
                    }
                }
            }

            post {
                val options: FileCreationOptions = call.receive()

                when (val result = fileLogic.createFile(options)) {
                    is FileCreationResult.Duplicate -> call.respond(HttpStatusCode.Conflict, mapOf("path" to options.path, "fileName" to options.fileName))
                    is FileCreationResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                    is FileCreationResult.NotFound -> call.respond(HttpStatusCode.NotFound, mapOf("path" to options.path))
                    is FileCreationResult.Success -> call.respond(HttpStatusCode.Created, mapOf("path" to options.path, "fileName" to options.fileName))
                }
            }

            delete("/{path}/{fileName}") {
                val options = FileDeleteOptions(
                    path = call.parameters["path"] ?: "",
                    fileName = call.parameters["fileName"] ?: ""
                )

                when (val result = fileLogic.deleteFile(options)) {
                    is FileDeleteResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                    is FileDeleteResult.IsADirectory -> call.respond(HttpStatusCode.BadRequest, mapOf("path" to options.path, "fileName" to options.fileName))
                    is FileDeleteResult.NotFound -> call.respond(HttpStatusCode.NotFound, mapOf("path" to options.path, "fileName" to options.fileName))
                    is FileDeleteResult.Success -> call.respond(HttpStatusCode.NoContent)
                }
            }

            delete("/{path}") {
                val options = DirectoryDeleteOptions(
                    path = call.parameters["path"] ?: "",
                    recursive = call.request.queryParameters["recursive"]?.toBoolean() ?: false
                )

                when (val result = fileLogic.deleteDirectory(options)) {
                    is DirectoryDeleteResult.DirectoryHasChildren -> call.respond(HttpStatusCode.BadRequest, mapOf("path" to options.path, "hasChildren" to true))
                    is DirectoryDeleteResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                    is DirectoryDeleteResult.IsAFile -> call.respond(HttpStatusCode.BadRequest, mapOf("path" to options.path, "hasChildren" to false))
                    is DirectoryDeleteResult.NotFound -> call.respond(HttpStatusCode.NotFound, mapOf("path" to options.path))
                    is DirectoryDeleteResult.Success -> call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
