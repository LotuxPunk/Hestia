package be.vandeas.logic.impl

import be.vandeas.domain.*
import be.vandeas.dto.DirectoryDeleteOptions
import be.vandeas.dto.FileCreationOptions
import be.vandeas.dto.FileDeleteOptions
import be.vandeas.dto.FileReadOptions
import be.vandeas.handler.FileHandler
import be.vandeas.logic.FileLogic
import io.ktor.util.*
import java.nio.file.Paths

class FileLogicImpl : FileLogic {
    override fun createFile(options: FileCreationOptions): FileCreationResult {
        return FileHandler.write(
            content = options.content.decodeBase64Bytes(),
            filePath = Paths.get(options.path, options.fileName)
        )
    }

    override fun deleteFile(fileDeleteOptions: FileDeleteOptions): FileDeleteResult {
        return FileHandler.deleteFile(
            filePath = Paths.get(fileDeleteOptions.path, fileDeleteOptions.fileName)
        )
    }

    override fun deleteDirectory(directoryDeleteOptions: DirectoryDeleteOptions): DirectoryDeleteResult {
        return FileHandler.deleteDirectory(
            filePath = Paths.get(directoryDeleteOptions.path),
            recursive = directoryDeleteOptions.recursive
        )
    }

    override fun readFile(fileReadOptions: FileReadOptions): FileBytesReadResult {
        return FileHandler.read(
            path = Paths.get(fileReadOptions.path, fileReadOptions.fileName)
        )
    }

    override fun getFile(fileReadOptions: FileReadOptions): FileReadResult {
        return FileHandler.get(
            path = Paths.get(fileReadOptions.path, fileReadOptions.fileName)
        )
    }
}
