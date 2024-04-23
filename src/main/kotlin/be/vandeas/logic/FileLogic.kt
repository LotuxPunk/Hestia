package be.vandeas.logic

import be.vandeas.FileCreationResult

interface FileLogic {
    fun createFile(data: ByteArray, path: String, fileName: String): FileCreationResult
    fun deleteFile(path: String, fileName: String): Boolean
    fun readFile(path: String, fileName: String): ByteArray
}
