package be.vandeas.service

import be.vandeas.domain.*
import be.vandeas.dto.*

interface FileService {
    fun createFile(token: String, options: Base64FileCreationOptions): FileCreationResult
    fun createFile(token: String, options: BytesFileCreationOptions): FileCreationResult
    fun deleteFile(token: String, fileDeleteOptions: FileDeleteOptions): FileDeleteResult
    fun deleteDirectory(token: String, directoryDeleteOptions: DirectoryDeleteOptions): DirectoryDeleteResult
    fun readFile(token: String, fileReadOptions: FileReadOptions): FileBytesReadResult
    fun getFile(token: String, fileReadOptions: FileReadOptions): FileReadResult
}
