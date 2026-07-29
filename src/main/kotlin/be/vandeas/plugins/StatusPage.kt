package be.vandeas.plugins

import be.vandeas.exception.AuthorizationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import java.io.IOException

fun Application.configureStatus() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when(cause) {
                is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest, cause.message ?: "Bad Request")
                is IllegalStateException -> call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Internal Server Error")
                is NotFoundException -> call.respond(HttpStatusCode.NotFound, cause.message ?: "Not Found")
                is AuthorizationException -> call.respond(HttpStatusCode.Unauthorized, cause.message ?: "Unauthorized")
                is IOException -> if (cause.isUploadSizeLimitExceeded()) {
                    call.respond(HttpStatusCode.PayloadTooLarge, "Upload exceeds the maximum allowed size")
                } else {
                    call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Internal Server Error")
                }
                else -> call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Internal Server Error")
            }
        }
    }

}

/**
 * Ktor has no dedicated exception for the multipart size limit, and reports it as a plain
 * [IOException] whose message depends on where the limit is hit: on the length a part declares, or
 * while the parser scans for the next boundary. The exception is raised by the parser itself, which
 * fails the whole call, so it cannot be mapped where the upload is read either.
 *
 * Both messages are matched rather than every [IOException], so that a failure to write the upload
 * keeps being reported as an internal error. Should Ktor reword them, an upload over the limit falls
 * back to an internal error, and the tests covering both cases fail.
 */
private fun IOException.isUploadSizeLimitExceeded(): Boolean = message?.let {
    it.contains("bytes exceeded while scanning for") || it.contains("Multipart content length exceeds limit")
} == true
