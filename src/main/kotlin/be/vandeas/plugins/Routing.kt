package be.vandeas.plugins

import be.vandeas.domain.*
import be.vandeas.dto.*
import be.vandeas.dto.ReadFileBytesResult.Companion.mapToReadFileBytesDto
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
                    val path = call.request.queryParameters["path"] ?: ""
                    val fileName = call.request.queryParameters["fileName"] ?: ""
                    val authorization = call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")
                    val accept = call.request.accept()?.let { ContentType.parse(it) } ?: ContentType.Application.Json

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
                            ContentType.Application.Json -> call.respond(HttpStatusCode.OK, result.mapToReadFileBytesDto())
                            ContentType.Application.OctetStream -> call.respondBytes(result.data)
                            else -> call.respond(HttpStatusCode.NotAcceptable, "Accept header must be application/json or application/octet-stream")
                        }
                    }
                }

                get("/embed") {
                    val path = call.request.queryParameters["path"] ?: ""
                    val fileName = call.request.queryParameters["fileName"] ?: ""
                    val authorization = call.request.queryParameters["token"] ?: call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")

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

                delete {
                    val path = call.request.queryParameters["path"] ?: throw IllegalArgumentException("path query parameter is required")
                    val fileName = call.request.queryParameters["fileName"]
                    val recursive = call.request.queryParameters["recursive"]?.toBoolean() ?: false

                    val authorization = call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")

                    if (fileName == null) {
                        val options = DirectoryDeleteOptions(
                            path = path,
                            recursive = recursive
                        )

                        when (val result = fileService.deleteDirectory(authorization, options)) {
                            is DirectoryDeleteResult.DirectoryHasChildren -> call.respond(HttpStatusCode.BadRequest, mapOf("path" to options.path, "hasChildren" to true))
                            is DirectoryDeleteResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                            is DirectoryDeleteResult.IsAFile -> call.respond(HttpStatusCode.BadRequest, mapOf("path" to options.path, "hasChildren" to false))
                            is DirectoryDeleteResult.NotFound -> call.respond(HttpStatusCode.NotFound, mapOf("path" to options.path))
                            is DirectoryDeleteResult.Success -> call.respond(HttpStatusCode.NoContent)
                        }
                    } else {
                        val options = FileDeleteOptions(
                            path = call.request.queryParameters["path"] ?: throw IllegalArgumentException("path query parameter is required"),
                            fileName = call.request.queryParameters["fileName"] ?: ""
                        )

                        when (val result = fileService.deleteFile(authorization, options)) {
                            is FileDeleteResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                            is FileDeleteResult.IsADirectory -> call.respond(HttpStatusCode.BadRequest, FileNameWithPath(path = options.path, fileName = options.fileName))
                            is FileDeleteResult.NotFound -> call.respond(HttpStatusCode.NotFound, FileNameWithPath(path = options.path, fileName = options.fileName))
                            is FileDeleteResult.Success -> call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
            route("/auth") {
                get("/token") {
                    val authorization = call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")
                    val path = call.request.queryParameters["path"] ?: throw IllegalArgumentException("path is required")
                    val fileName = call.request.queryParameters["fileName"] ?: throw IllegalArgumentException("fileName is required")

                    authLogic.getOneTimeToken(authorization, path, fileName).let {
                        call.respond(HttpStatusCode.OK, mapOf("token" to it))
                    }
                }
            }
        }
    }
}
