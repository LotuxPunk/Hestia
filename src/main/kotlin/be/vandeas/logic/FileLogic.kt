package be.vandeas.logic

import be.vandeas.domain.input.DirectoryDeleteOptions
import be.vandeas.domain.input.FileCreationOptions
import be.vandeas.domain.input.FileDeleteOptions
import be.vandeas.domain.input.FileReadOptions
import be.vandeas.domain.output.DirectoryDeleteResult
import be.vandeas.domain.output.FileCreationResult
import be.vandeas.domain.output.FileDeleteResult
import be.vandeas.domain.output.FileReadResult

interface FileLogic {
    fun createFile(options: FileCreationOptions): FileCreationResult
    fun deleteFile(fileDeleteOptions: FileDeleteOptions): FileDeleteResult
    fun deleteDirectory(directoryDeleteOptions: DirectoryDeleteOptions): DirectoryDeleteResult
    fun readFile(fileReadOptions: FileReadOptions): FileReadResult
}
