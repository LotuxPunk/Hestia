package be.vandeas.service

import be.vandeas.domain.*
import be.vandeas.dto.DirectoryDeleteOptions
import be.vandeas.dto.FileCreationOptions
import be.vandeas.dto.FileDeleteOptions
import be.vandeas.dto.FileReadOptions

interface FileService {
    fun createFile(token: String, options: FileCreationOptions): FileCreationResult
    fun deleteFile(token: String, fileDeleteOptions: FileDeleteOptions): FileDeleteResult
    fun deleteDirectory(token: String, directoryDeleteOptions: DirectoryDeleteOptions): DirectoryDeleteResult
    fun readFile(token: String, fileReadOptions: FileReadOptions): FileBytesReadResult
    fun getFile(token: String, fileReadOptions: FileReadOptions): FileReadResult
}
