package be.vandeas.plugins

import be.vandeas.exception.AuthorizationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatus() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when(cause) {
                is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest, cause.message ?: "Bad Request")
                is IllegalStateException -> call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Internal Server Error")
                is NotFoundException -> call.respond(HttpStatusCode.NotFound, cause.message ?: "Not Found")
                is AuthorizationException -> call.respond(HttpStatusCode.Unauthorized, cause.message ?: "Unauthorized")
                else -> call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Internal Server Error")
            }
        }
    }

}
