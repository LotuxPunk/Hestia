package be.vandeas

import be.vandeas.plugins.configureHTTP
import be.vandeas.plugins.configureMonitoring
import be.vandeas.plugins.configureRouting
import be.vandeas.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureRouting()
}
