package be.vandeas.plugins

import be.vandeas.controller.v1.fileControllerV1
import be.vandeas.controller.v2.fileControllerV2
import be.vandeas.logic.AuthLogic
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting() {
    install(PartialContent)
    install(AutoHeadResponse)

    val authLogic by inject<AuthLogic>()

    routing {
        route("/v1") {
            fileControllerV1()
            route("/auth") {
                get("/token") {
                    val authorization = call.request.authorization()
                        ?: throw IllegalArgumentException("Authorization header is required")
                    val path =
                        call.request.queryParameters["path"] ?: throw IllegalArgumentException("path is required")
                    val fileName = call.request.queryParameters["fileName"]
                        ?: throw IllegalArgumentException("fileName is required")

                    authLogic.getOneTimeToken(authorization, path, fileName).let {
                        call.respond(HttpStatusCode.OK, mapOf("token" to it))
                    }
                }
            }
        }
        route("/v2") {
            fileControllerV2()
            route("/auth") {
                get("/token") {
                    val authorization = call.request.authorization()
                        ?: throw IllegalArgumentException("Authorization header is required")
                    val lifeTime = call.request.queryParameters["lifeTime"]?.toInt()
                        ?: throw IllegalArgumentException("lifeTime is required")

                    authLogic.getJwtToken(authorization, lifeTime.seconds).let {
                        call.respond(HttpStatusCode.OK, mapOf("token" to it))
                    }
                }
            }
        }
    }
}
