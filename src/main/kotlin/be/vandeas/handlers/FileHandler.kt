package be.vandeas.handlers

import io.ktor.util.logging.*
import io.ktor.utils.io.errors.*
import java.net.URI
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

sealed interface DirectoryCreationResult {
    data class Success(val path: Path) : DirectoryCreationResult
    data class Duplicate(val path: Path) : DirectoryCreationResult
    data class Failure(val message: String) : DirectoryCreationResult
}

sealed interface FileReadResult {
    data class Success(val data: ByteArray) : FileReadResult
    data class NotFound(val path: String) : FileReadResult
    data class Failure(val message: String) : FileReadResult
}

object FileHandler {

    private val LOGGER = KtorSimpleLogger("be.vandeas.handlers.FileHandler")

    private val BASE_DIRECTORY: Path = Path.of(URI.create("file://${System.getenv("BASE_DIRECTORY")}"))

    fun createDirectory(path: String): DirectoryCreationResult {
        try {
            return DirectoryCreationResult.Success(Files.createDirectory(BASE_DIRECTORY.resolve(path)))
        } catch (e: IOException) {
            LOGGER.error(e)
            return DirectoryCreationResult.Failure(e.message ?: "An error occurred while creating the directory.")
        } catch (e: InvalidPathException) {
            LOGGER.error(e)
            return DirectoryCreationResult.Failure(e.message ?: "The path is invalid.")
        }

    }

    fun read(path: String): ByteArray {
        try {
            return FileReadResult.Success(Files.readAllBytes(BASE_DIRECTORY.resolve(path)).toList().toByteArray())
        } catch (e: IOException) {
            LOGGER.error(e)
            return FileReadResult.Failure(e.message ?: "An error occurred while reading the file.")
        } catch (e: InvalidPathException) {
            LOGGER.error(e)
            return FileReadResult.Failure(e.message ?: "The path is invalid.")
        }

    }
}
