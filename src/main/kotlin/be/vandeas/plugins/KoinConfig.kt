package be.vandeas.plugins

import be.vandeas.logic.AuthLogic
import be.vandeas.logic.FileLogic
import be.vandeas.logic.impl.AuthLogicImpl
import be.vandeas.logic.impl.FileLogicImpl
import be.vandeas.service.FileService
import be.vandeas.service.impl.FileServiceImpl
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.KoinApplicationStarted
import org.koin.ktor.plugin.KoinApplicationStopPreparing
import org.koin.ktor.plugin.KoinApplicationStopped
import org.koin.logger.slf4jLogger

val appModule = module {
    single<FileLogic> {
        FileLogicImpl()
    }

    single<AuthLogic> {
        AuthLogicImpl()
    }

    single<FileService> {
        FileServiceImpl(get(), get())
    }
}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
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
