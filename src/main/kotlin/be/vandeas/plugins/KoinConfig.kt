package be.vandeas.plugins

import be.vandeas.handler.FileHandler
import be.vandeas.logic.AuthLogic
import be.vandeas.logic.FileLogic
import be.vandeas.logic.impl.AuthLogicImpl
import be.vandeas.logic.impl.FileLogicImpl
import be.vandeas.service.v1.FileService as FileServiceV1
import be.vandeas.service.v2.FileService as FileServiceV2
import be.vandeas.service.v1.impl.FileServiceImpl as FileServiceImplV1
import be.vandeas.service.v2.impl.FileServiceImpl as FileServiceImplV2
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.KoinApplicationStarted
import org.koin.ktor.plugin.KoinApplicationStopPreparing
import org.koin.ktor.plugin.KoinApplicationStopped
import org.koin.logger.slf4jLogger

fun appModule(environment: ApplicationEnvironment) = module {
    single<FileLogic> {
        FileLogicImpl(
            privateFileHandler = FileHandler(System.getenv("BASE_DIRECTORY")),
            publicFileHandler = FileHandler(System.getenv("PUBLIC_DIRECTORY")),
        )
    }

    single<AuthLogic> {
        AuthLogicImpl(
            secret = System.getenv("JWT_SECRET"),
            issuer = System.getenv("JWT_ISSUER"),
            audience = System.getenv("JWT_AUDIENCE"),
            realm = System.getenv("JWT_REALM"),
        )
    }

    single<FileServiceV1> {
        FileServiceImplV1(get(), get())
    }

    single<FileServiceV2> {
        FileServiceImplV2(get(), get())
    }
}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule(environment))
    }

    environment.monitor.subscribe(KoinApplicationStarted) {
        log.info("Koin started.")
    }

    environment.monitor.subscribe(KoinApplicationStopPreparing) {
        log.info("Koin stopping...")
    }

    environment.monitor.subscribe(KoinApplicationStopped) {
        log.info("Koin stopped.")
    }
}
