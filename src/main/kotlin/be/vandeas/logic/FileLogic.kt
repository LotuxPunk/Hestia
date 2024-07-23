package be.vandeas.logic

import be.vandeas.domain.*
import be.vandeas.dto.*

interface FileLogic {
    fun createFile(options: Base64FileCreationOptions): FileCreationResult
    fun createFile(options: BytesFileCreationOptions): FileCreationResult
    fun deleteFile(fileDeleteOptions: FileDeleteOptions): FileDeleteResult
    fun deleteDirectory(directoryDeleteOptions: DirectoryDeleteOptions): DirectoryDeleteResult
    fun readFile(fileReadOptions: FileReadOptions): FileBytesReadResult
    fun getFile(fileReadOptions: FileReadOptions): FileReadResult
}
