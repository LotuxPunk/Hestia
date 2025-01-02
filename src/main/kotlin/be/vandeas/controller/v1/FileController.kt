package be.vandeas.controller.v1

import be.vandeas.domain.*
import be.vandeas.dto.*
import be.vandeas.dto.ReadFileBytesResult.Companion.mapToReadFileBytesDto
import be.vandeas.exception.AuthorizationException
import be.vandeas.service.v1.FileService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.fileControllerV1() = route("/file") {
    val fileService by inject<FileService>()

    get {
        val path = call.request.queryParameters["path"] ?: ""
        val fileName = call.request.queryParameters["fileName"] ?: ""
        val authorization = call.request.authorization() ?: throw AuthorizationException("Authorization header is required")
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

            is FileBytesReadResult.BadRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
        }
    }

    get("/embed") {
        val path = call.request.queryParameters["path"] ?: ""
        val fileName = call.request.queryParameters["fileName"] ?: ""
        val downloadFileName = call.request.queryParameters["download"] ?: ""
        val authorization = call.request.queryParameters["token"] ?: call.request.authorization() ?: throw IllegalArgumentException("Authorization header is required")

        if (path.isBlank() || fileName.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("path" to path, "fileName" to fileName))
            return@get
        }

        when (val result = fileService.getFile(authorization, FileReadOptions(path = path, fileName = fileName))) {
            is FileReadResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
            is FileReadResult.NotFound -> call.respond(HttpStatusCode.NotFound, FileNameWithPath(path = path, fileName = fileName))
            is FileReadResult.Success -> {
                if (downloadFileName.isNotBlank()) {
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, downloadFileName)
                            .toString()
                    )
                } else {
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName)
                            .toString()
                    )
                }
                call.respondFile(result.file)
            }

            is FileReadResult.BadRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
        }
    }

    post {
        val options: Base64FileCreationOptions = call.receive()
        val authorization = call.request.authorization() ?: throw AuthorizationException("Authorization header is required")

        when (val result = fileService.createFile(authorization, options)) {
            is FileCreationResult.Duplicate -> call.respond(HttpStatusCode.Conflict, FileNameWithPath(path = options.path, fileName = options.fileName))
            is FileCreationResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
            is FileCreationResult.NotFound -> call.respond(HttpStatusCode.NotFound, mapOf("path" to options.path))
            is FileCreationResult.Success -> call.respond(HttpStatusCode.Created, FileNameWithPath(path = options.path, fileName = options.fileName))
            is FileCreationResult.BadRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
        }
    }

    post("/upload") {
        val authorization = call.request.authorization() ?: throw AuthorizationException("Authorization header is required")
        val multipart = call.receiveMultipart()

        var fileName: String? = null
        var path: String? = null
        var data: ByteArray? = null

        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    when (part.name) {
                        "path" -> path = part.value
                        "fileName" -> fileName = part.value
                    }
                }
                is PartData.FileItem -> {
                    data = part.streamProvider().readBytes()
                }
                else -> throw IllegalArgumentException("Unsupported part type: ${part::class.simpleName}")
            }
            part.dispose()
        }

        requireNotNull(fileName) { "fileName is required" }
        requireNotNull(path) { "path is required" }
        requireNotNull(data) { "data is required" }

        val options = BytesFileCreationOptions(
            path = path!!,
            fileName = fileName!!,
            content = data!!
        )

        when (val result = fileService.createFile(authorization, options)) {
            is FileCreationResult.Duplicate -> call.respond(HttpStatusCode.Conflict, FileNameWithPath(path = options.path, fileName = options.fileName))
            is FileCreationResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
            is FileCreationResult.NotFound -> call.respond(HttpStatusCode.NotFound, mapOf("path" to options.path))
            is FileCreationResult.Success -> call.respond(HttpStatusCode.Created, FileNameWithPath(path = options.path, fileName = options.fileName))
            is FileCreationResult.BadRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
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
                recursive = recursive,
                public = false
            )

            when (val result = fileService.deleteDirectory(authorization, options)) {
                is DirectoryDeleteResult.DirectoryHasChildren -> call.respond(HttpStatusCode.BadRequest, mapOf("path" to options.path, "hasChildren" to true))
                is DirectoryDeleteResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                is DirectoryDeleteResult.IsAFile -> call.respond(HttpStatusCode.BadRequest, mapOf("path" to options.path, "hasChildren" to false))
                is DirectoryDeleteResult.NotFound -> call.respond(HttpStatusCode.NotFound, mapOf("path" to options.path))
                is DirectoryDeleteResult.Success -> call.respond(HttpStatusCode.NoContent)
                is DirectoryDeleteResult.BadRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
            }
        } else {
            val options = FileDeleteOptions(
                path = path,
                fileName = fileName,
                public = false
            )

            when (val result = fileService.deleteFile(authorization, options)) {
                is FileDeleteResult.Failure -> call.respond(HttpStatusCode.InternalServerError, result.message)
                is FileDeleteResult.IsADirectory -> call.respond(HttpStatusCode.BadRequest, FileNameWithPath(path = options.path, fileName = options.fileName))
                is FileDeleteResult.NotFound -> call.respond(HttpStatusCode.NotFound, FileNameWithPath(path = options.path, fileName = options.fileName))
                is FileDeleteResult.Success -> call.respond(HttpStatusCode.NoContent)
                is FileDeleteResult.BadRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
            }
        }
    }
}
