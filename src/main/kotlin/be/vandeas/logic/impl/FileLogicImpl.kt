package be.vandeas.logic.impl

import be.vandeas.domain.input.DirectoryDeleteOptions
import be.vandeas.domain.input.FileCreationOptions
import be.vandeas.domain.input.FileDeleteOptions
import be.vandeas.domain.input.FileReadOptions
import be.vandeas.domain.output.DirectoryDeleteResult
import be.vandeas.domain.output.FileCreationResult
import be.vandeas.domain.output.FileDeleteResult
import be.vandeas.domain.output.FileReadResult
import be.vandeas.handlers.FileHandler
import be.vandeas.logic.FileLogic
import java.nio.file.Paths

class FileLogicImpl : FileLogic {
    override fun createFile(options: FileCreationOptions): FileCreationResult {
        return FileHandler.write(
            content = options.content,
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

    override fun readFile(fileReadOptions: FileReadOptions): FileReadResult {
        return FileHandler.read(
            path = Paths.get(fileReadOptions.path, fileReadOptions.fileName)
        )
    }

}
