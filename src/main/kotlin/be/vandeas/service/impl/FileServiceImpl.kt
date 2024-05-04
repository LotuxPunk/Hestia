package be.vandeas.service.impl

import be.vandeas.domain.*
import be.vandeas.dto.DirectoryDeleteOptions
import be.vandeas.dto.FileCreationOptions
import be.vandeas.dto.FileDeleteOptions
import be.vandeas.dto.FileReadOptions
import be.vandeas.logic.AuthLogic
import be.vandeas.logic.FileLogic
import be.vandeas.service.FileService
import java.nio.file.Path

class FileServiceImpl(
    private val fileLogic: FileLogic,
    private val authLogic: AuthLogic
) : FileService {
    override fun createFile(token: String, options: FileCreationOptions): FileCreationResult =
        authLogic.guard(token, Path.of(options.path, options.fileName)) {
            fileLogic.createFile(options)
        }

    override fun deleteFile(token: String, fileDeleteOptions: FileDeleteOptions): FileDeleteResult =
        authLogic.guard(token, Path.of(fileDeleteOptions.path, fileDeleteOptions.fileName)) {
            fileLogic.deleteFile(fileDeleteOptions)
        }

    override fun deleteDirectory(token: String, directoryDeleteOptions: DirectoryDeleteOptions): DirectoryDeleteResult =
        authLogic.guard(token, Path.of(directoryDeleteOptions.path)) {
            fileLogic.deleteDirectory(directoryDeleteOptions)
        }

    override fun readFile(token: String, fileReadOptions: FileReadOptions): FileBytesReadResult =
        authLogic.guard(token, Path.of(fileReadOptions.path, fileReadOptions.fileName)) {
            fileLogic.readFile(fileReadOptions)
        }

    override fun getFile(token: String, fileReadOptions: FileReadOptions): FileReadResult =
        authLogic.guard(token, Path.of(fileReadOptions.path, fileReadOptions.fileName)) {
            fileLogic.getFile(fileReadOptions)
        }
}
