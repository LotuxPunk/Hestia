package be.vandeas.handlers

import be.vandeas.domain.output.DirectoryDeleteResult
import be.vandeas.domain.output.FileCreationResult
import be.vandeas.domain.output.FileDeleteResult
import be.vandeas.domain.output.FileReadResult
import io.ktor.util.logging.*
import io.ktor.utils.io.errors.*
import java.net.URI
import java.nio.file.*
import kotlin.io.FileAlreadyExistsException
import kotlin.io.path.*

object FileHandler {
    private val LOGGER = KtorSimpleLogger("be.vandeas.handlers.FileHandler")
    private val BASE_DIRECTORY: Path = Path.of(URI.create("file://${System.getenv("BASE_DIRECTORY")}"))

    /**
     * Writes a file.
     *
     * @param path The path to the file to write.
     * @param content The content to write to the file.
     *
     * @return A [FileCreationResult] indicating the result of the operation.
     */
    fun write(content: ByteArray, filePath: Path): FileCreationResult {
        try {
            if (!filePath.parent.exists()) {
                if (!filePath.parent.toFile().mkdirs()) {
                    LOGGER.error("Failed to create directory: {}", filePath.parent)
                    return FileCreationResult.Failure("Failed to create directory: ${filePath.parent}")
                }
                LOGGER.debug("Created directory: {}", filePath.parent)
            }
            return FileCreationResult.Success(Files.write(filePath, content, StandardOpenOption.CREATE_NEW))
        } catch (e: InvalidPathException) {
            LOGGER.error(e)
            return FileCreationResult.NotFound(filePath)
        } catch (e: FileAlreadyExistsException) {
            LOGGER.error(e)
            return FileCreationResult.Duplicate(filePath)
        } catch (e: Exception) {
            LOGGER.error(e)
            return FileCreationResult.Failure(e.message ?: "An error occurred while writing the file.")
        }
    }

    /**
     * Reads a file.
     *
     * @param path The path to the file to read.
     *
     * @return A [FileReadResult] indicating the result of the operation.
     */
    fun read(path: Path): FileReadResult {
        val filePath = BASE_DIRECTORY.resolve(path)

        try {
            return FileReadResult.Success(Files.readAllBytes(filePath).toList().toByteArray())
        } catch (e: InvalidPathException) {
            LOGGER.error(e)
            return FileReadResult.NotFound(filePath)
        } catch (e: Exception) {
            LOGGER.error(e)
            return FileReadResult.Failure(e.message ?: "An error occurred while reading the file.")
        }
    }

    /**
     * Deletes a file.
     *
     * @param filePath The path to the file to delete.
     *
     * @return A [FileDeleteResult] indicating the result of the operation.
     */
    fun deleteFile(filePath: Path): FileDeleteResult {
        try {
            if (filePath.exists()) {
                if (filePath.isDirectory()) {
                    return FileDeleteResult.IsADirectory(filePath)
                }
                filePath.deleteExisting()
            }
            return FileDeleteResult.Success(filePath)
        } catch (e: InvalidPathException) {
            LOGGER.error(e)
            return FileDeleteResult.Success(filePath)
        } catch (e: NoSuchFileException) {
            LOGGER.error(e)
            return FileDeleteResult.NotFound(filePath)
        } catch (e: Exception) {
            LOGGER.error(e)
            return FileDeleteResult.Failure(e.message ?: "An error occurred while reading the file.")
        }
    }

    /**
     * Deletes a directory and its contents.
     *
     * @param path The path to the directory to delete.
     * @param recursive Whether to delete the directory and its contents recursively. If `false` and the directory has children directories, the directory will not be deleted. If `true`, the directory and its contents will be deleted recursively. Recursive deletion is not atomic and may fail partway through the process.
     *
     * @return A [DirectoryDeleteResult] indicating the result of the operation.
     */
    @OptIn(ExperimentalPathApi::class)
    fun deleteDirectory(filePath: Path, recursive: Boolean): DirectoryDeleteResult {
        try {
            if (filePath.exists()) {
                if (filePath.isDirectory()) {
                    if (recursive) {
                        filePath.deleteRecursively()
                    } else {
                        filePath.toFile().listFiles().let { children ->
                            checkNotNull(children)
                            if (children.any { child -> child.isDirectory }) {
                                return DirectoryDeleteResult.DirectoryHasChildren(filePath)
                            }
                            filePath.deleteExisting()
                        }
                    }
                    return DirectoryDeleteResult.Success(filePath)
                }
                return DirectoryDeleteResult.IsAFile(filePath)
            }
            return DirectoryDeleteResult.Success(filePath)
        } catch (e: IOException) {
            LOGGER.error(e)
            return DirectoryDeleteResult.Failure(e.message ?: "An error occurred while reading the file.")
        } catch (e: NoSuchFileException) {
            LOGGER.error(e)
            return DirectoryDeleteResult.NotFound(filePath)
        } catch (e: InvalidPathException) {
            LOGGER.error(e)
            return DirectoryDeleteResult.Success(filePath)
        }
    }
}
