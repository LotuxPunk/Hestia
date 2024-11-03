package be.vandeas.logic.impl

import be.vandeas.domain.*
import be.vandeas.dto.*
import be.vandeas.handler.FileHandler
import be.vandeas.logic.FileLogic
import io.ktor.util.*
import java.nio.file.Paths

class FileLogicImpl(
    private val privateFileHandler: FileHandler,
    private val publicFileHandler: FileHandler
) : FileLogic {

    private fun FileVisibilityOptions.fileHandler() = if (public) publicFileHandler else privateFileHandler

    override fun createFile(options: Base64FileCreationOptions): FileCreationResult {
        return options.fileHandler().write(
            content = options.content.decodeBase64Bytes(),
            filePath = Paths.get(options.path, options.fileName)
        )
    }

    override fun createFile(options: BytesFileCreationOptions): FileCreationResult {
        return options.fileHandler().write(
            content = options.content,
            filePath = Paths.get(options.path, options.fileName)
        )
    }

    override fun deleteFile(fileDeleteOptions: FileDeleteOptions): FileDeleteResult {
        return fileDeleteOptions.fileHandler().deleteFile(
            path = Paths.get(fileDeleteOptions.path, fileDeleteOptions.fileName)
        )
    }

    override fun deleteDirectory(directoryDeleteOptions: DirectoryDeleteOptions): DirectoryDeleteResult {
        return directoryDeleteOptions.fileHandler().deleteDirectory(
            path = Paths.get(directoryDeleteOptions.path),
            recursive = directoryDeleteOptions.recursive
        )
    }

    override fun readFile(fileReadOptions: FileReadOptions): FileBytesReadResult {
        return privateFileHandler.read(
            path = Paths.get(fileReadOptions.path, fileReadOptions.fileName)
        )
    }

    override fun getFile(fileReadOptions: FileReadOptions): FileReadResult {
        return privateFileHandler.get(
            path = Paths.get(fileReadOptions.path, fileReadOptions.fileName)
        )
    }
}
