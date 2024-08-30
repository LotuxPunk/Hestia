package be.vandeas.service.v2.impl

import be.vandeas.domain.*
import be.vandeas.dto.*
import be.vandeas.logic.AuthLogic
import be.vandeas.logic.FileLogic
import be.vandeas.service.v2.FileService

class FileServiceImpl(
    private val fileLogic: FileLogic,
    private val authLogic: AuthLogic
) : FileService {
    override fun getFile(jwt: String, fileReadOptions: FileReadOptions): FileReadResult =
        authLogic.guard(jwt) {
            fileLogic.getFile(fileReadOptions)
        }
}
