package be.vandeas.handler

import be.vandeas.domain.*
import io.ktor.util.logging.*
import kotlinx.io.IOException
import java.net.URI
import java.nio.file.*
import kotlin.io.path.*

class FileHandler(
    directory: String
) {
    private val LOGGER = KtorSimpleLogger("be.vandeas.handlers.FileHandler")
    private val BASE_DIRECTORY: Path = Path.of(URI.create("file://$directory"))

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
            val path = resolvePath(filePath)
            if (!path.parent.exists()) {
                if (!path.parent.toFile().mkdirs()) {
                    LOGGER.error("Failed to create directory: {}", path.parent)
                    return FileCreationResult.Failure("Failed to create directory: ${path.parent}")
                }
                LOGGER.debug("Created directory: {}", path.parent)
            }
            return FileCreationResult.Success(Files.write(path, content, StandardOpenOption.CREATE_NEW))
        } catch (e: InvalidPathException) {
            LOGGER.error(e)
            return FileCreationResult.NotFound(filePath)
        } catch (e: IllegalArgumentException) {
            LOGGER.error(e)
            return FileCreationResult.BadRequest("Invalid path: $filePath")
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
     * @return A [FileBytesReadResult] indicating the result of the operation.
     */
    fun get(path: Path): FileReadResult {
        try {
            val filePath = resolvePath(path)
            return FileReadResult.Success(filePath.toFile())
        } catch (e: UnsupportedOperationException) {
            LOGGER.error(e)
            return FileReadResult.NotFound(path)
        } catch (e: IllegalArgumentException) {
            LOGGER.error(e)
            return FileReadResult.BadRequest("Invalid path: $path")
        } catch (e: Exception) {
            LOGGER.error(e)
            return FileReadResult.Failure(e.message ?: "An error occurred while reading the file.")
        }
    }

    /**
     * Reads a file.
     *
     * @param path The path to the file to read.
     *
     * @return A [FileBytesReadResult] indicating the result of the operation.
     */
    fun read(path: Path): FileBytesReadResult {
        try {
            val filePath = resolvePath(path)
            return FileBytesReadResult.Success(Files.readAllBytes(filePath).toList().toByteArray(), filePath)
        } catch (e: NoSuchFileException) {
            LOGGER.error(e)
            return FileBytesReadResult.NotFound(path)
        } catch (e: IllegalArgumentException) {
            LOGGER.error(e)
            return FileBytesReadResult.BadRequest("Invalid path: $path")
        } catch (e: InvalidPathException) {
            LOGGER.error(e)
            return FileBytesReadResult.NotFound(path)
        } catch (e: Exception) {
            LOGGER.error(e)
            return FileBytesReadResult.Failure(e.message ?: "An error occurred while reading the file.")
        }
    }

    /**
     * Deletes a file.
     *
     * @param path The path to the file to delete.
     *
     * @return A [FileDeleteResult] indicating the result of the operation.
     */
    fun deleteFile(path: Path): FileDeleteResult {
        try {
            val filePath = resolvePath(path)
            if (filePath.exists()) {
                if (filePath.isDirectory()) {
                    return FileDeleteResult.IsADirectory(filePath)
                }
                val success = filePath.toFile().delete()
                if (!success) {
                    return FileDeleteResult.Failure("Failed to delete file: $filePath")
                }
                return FileDeleteResult.Success(filePath)
            }
            return FileDeleteResult.NotFound(filePath)
        } catch (e: InvalidPathException) {
            LOGGER.error(e)
            return FileDeleteResult.Success(path)
        } catch (e: IllegalArgumentException) {
            LOGGER.error(e)
            return FileDeleteResult.BadRequest("Invalid path: $path")
        } catch (e: NoSuchFileException) {
            LOGGER.error(e)
            return FileDeleteResult.NotFound(path)
        } catch (e: Exception) {
            LOGGER.error(e)
            return FileDeleteResult.Failure(e.message ?: "An error occurred while deleting the file.")
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
    fun deleteDirectory(path: Path, recursive: Boolean): DirectoryDeleteResult {
        try {
            val filePath = resolvePath(path)
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
            return DirectoryDeleteResult.NotFound(filePath)
        } catch (e: IOException) {
            LOGGER.error(e)
            return DirectoryDeleteResult.Failure(e.message ?: "An error occurred while reading the file.")
        } catch (e: IllegalArgumentException) {
            LOGGER.error(e)
            return DirectoryDeleteResult.BadRequest("Invalid path: $path")
        } catch (e: NoSuchFileException) {
            LOGGER.error(e)
            return DirectoryDeleteResult.NotFound(path)
        } catch (e: InvalidPathException) {
            LOGGER.error(e)
            return DirectoryDeleteResult.Success(path)
        }
    }

    private fun resolvePath(path: Path): Path {
        val resolvedPath = BASE_DIRECTORY.resolve(path).normalize()
        if (!resolvedPath.startsWith(BASE_DIRECTORY.normalize())) {
            LOGGER.error("Resolved path is outside of the base directory: {}", resolvedPath)
            throw IllegalArgumentException("Resolved path is outside of the base directory: $resolvedPath")
        }
        return resolvedPath
    }
}
