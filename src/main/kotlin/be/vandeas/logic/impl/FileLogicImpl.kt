package be.vandeas.logic.impl

import be.vandeas.FileCreationResult
import be.vandeas.logic.FileLogic
import java.io.File
import java.nio.file.Files

class FileLogicImpl : FileLogic {
    override fun createFile(data: ByteArray, path: String, fileName: String): FileCreationResult {
        Files.createFile()
        TODO("Not yet implemented")
    }

    override fun deleteFile(path: String, fileName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun readFile(path: String, fileName: String): ByteArray {
        TODO("Not yet implemented")
    }
}
