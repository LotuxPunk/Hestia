package be.vandeas.plugins

import be.vandeas.domain.*
import be.vandeas.dto.*
import be.vandeas.logic.AuthLogic
import be.vandeas.service.FileService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    install(PartialContent)
    install(AutoHeadResponse)

    val fileService by inject<FileService>()
    val authLogic by inject<AuthLogic>()

    routing {
        route("/v1") {
            route("/file") {
                get {
                    val options: FileReadOptions = call.receive()
                    val authorization = call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")
                    val accept = call.request.accept()

                    when (val result = fileService.readFile(authorization, options)) {
                        is FileBytesReadResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                        is FileBytesReadResult.NotFound -> call.respond(HttpStatusCode.NotFound, FileNameWithPath(path = options.path, fileName = options.fileName))
                        is FileBytesReadResult.Success -> when(accept) {
                            ContentType.Application.Json.contentType -> call.respond(HttpStatusCode.OK, mapOf("content" to result.data.encodeBase64(), "fileName" to options.fileName))
                            ContentType.Application.OctetStream.contentType -> call.respondBytes(result.data)
                            else -> call.respond(HttpStatusCode.NotAcceptable, "Accept header must be application/json or application/octet-stream")
                        }
                    }
                }
                get("/{path}/{fileName}") {
                    val path = call.parameters["path"] ?: ""
                    val fileName = call.parameters["fileName"] ?: ""
                    val authorization = call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")
                    val accept = call.request.accept()

                    if (path.isBlank() || fileName.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("path" to path, "fileName" to fileName))
                        return@get
                    }

                    val options = FileReadOptions(
                        path = path,
                        fileName = fileName
                    )

                    when (val result = fileService.readFile(authorization, options)) {
                        is FileBytesReadResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                        is FileBytesReadResult.NotFound -> call.respond(HttpStatusCode.NotFound, FileNameWithPath(path = options.path, fileName = options.fileName))
                        is FileBytesReadResult.Success -> when(accept) {
                            ContentType.Application.Json.contentType -> call.respond(HttpStatusCode.OK, mapOf("content" to result.data.encodeBase64(), "fileName" to options.fileName))
                            ContentType.Application.OctetStream.contentType -> call.respondBytes(result.data)
                            else -> call.respond(HttpStatusCode.NotAcceptable, "Accept header must be application/json or application/octet-stream")
                        }
                    }
                }

                get("/embed/{path}/{fileName}") {
                    val path = call.parameters["path"] ?: ""
                    val fileName = call.parameters["fileName"] ?: ""
                    val authorization = call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")

                    if (path.isBlank() || fileName.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("path" to path, "fileName" to fileName))
                        return@get
                    }

                    when (val result = fileService.getFile(authorization, FileReadOptions(path = path, fileName = fileName))) {
                        is FileReadResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                        is FileReadResult.NotFound -> call.respond(HttpStatusCode.NotFound, FileNameWithPath(path = path, fileName = fileName))
                        is FileReadResult.Success -> {
                            call.response.header(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName)
                                    .toString()
                            )
                            call.respondFile(result.file)
                        }
                    }
                }

                post {
                    val options: FileCreationOptions = call.receive()
                    val authorization = call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")

                    when (val result = fileService.createFile(authorization, options)) {
                        is FileCreationResult.Duplicate -> call.respond(HttpStatusCode.Conflict, FileNameWithPath(path = options.path, fileName = options.fileName))
                        is FileCreationResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                        is FileCreationResult.NotFound -> call.respond(HttpStatusCode.NotFound, mapOf("path" to options.path))
                        is FileCreationResult.Success -> call.respond(HttpStatusCode.Created, FileNameWithPath(path = options.path, fileName = options.fileName))
                    }
                }

                delete("/{path}/{fileName}") {
                    val options = FileDeleteOptions(
                        path = call.parameters["path"] ?: "",
                        fileName = call.parameters["fileName"] ?: ""
                    )
                    val authorization = call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")

                    when (val result = fileService.deleteFile(authorization, options)) {
                        is FileDeleteResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                        is FileDeleteResult.IsADirectory -> call.respond(HttpStatusCode.BadRequest, FileNameWithPath(path = options.path, fileName = options.fileName))
                        is FileDeleteResult.NotFound -> call.respond(HttpStatusCode.NotFound, FileNameWithPath(path = options.path, fileName = options.fileName))
                        is FileDeleteResult.Success -> call.respond(HttpStatusCode.NoContent)
                    }
                }

                delete("/{path}") {
                    val options = DirectoryDeleteOptions(
                        path = call.parameters["path"] ?: "",
                        recursive = call.request.queryParameters["recursive"]?.toBoolean() ?: false
                    )
                    val authorization = call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")

                    when (val result = fileService.deleteDirectory(authorization, options)) {
                        is DirectoryDeleteResult.DirectoryHasChildren -> call.respond(HttpStatusCode.BadRequest, mapOf("path" to options.path, "hasChildren" to true))
                        is DirectoryDeleteResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                        is DirectoryDeleteResult.IsAFile -> call.respond(HttpStatusCode.BadRequest, mapOf("path" to options.path, "hasChildren" to false))
                        is DirectoryDeleteResult.NotFound -> call.respond(HttpStatusCode.NotFound, mapOf("path" to options.path))
                        is DirectoryDeleteResult.Success -> call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
            route("/auth") {
                get("/token/{path}/{fileName}") {
                    val authorization = call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")
                    val path = call.parameters["path"] ?: throw IllegalArgumentException("path is required")
                    val fileName = call.parameters["fileName"] ?: throw IllegalArgumentException("fileName is required")

                    authLogic.getOneTimeToken(authorization, path, fileName).let {
                        call.respond(HttpStatusCode.OK, mapOf("token" to it))
                    }
                }
            }
        }
    }
}
