package be.vandeas.logic

import be.vandeas.domain.*
import be.vandeas.dto.DirectoryDeleteOptions
import be.vandeas.dto.FileCreationOptions
import be.vandeas.dto.FileDeleteOptions
import be.vandeas.dto.FileReadOptions

interface FileLogic {
    fun createFile(options: FileCreationOptions): FileCreationResult
    fun deleteFile(fileDeleteOptions: FileDeleteOptions): FileDeleteResult
    fun deleteDirectory(directoryDeleteOptions: DirectoryDeleteOptions): DirectoryDeleteResult
    fun readFile(fileReadOptions: FileReadOptions): FileBytesReadResult
    fun getFile(fileReadOptions: FileReadOptions): FileReadResult
}
