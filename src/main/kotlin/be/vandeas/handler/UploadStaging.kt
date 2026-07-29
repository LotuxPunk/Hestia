package be.vandeas.handler

import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

/**
 * Copies [content] to a temporary file, in the directory pointed to by `java.io.tmpdir`.
 *
 * Uploads are staged on disk rather than held in memory: a multipart part can only be read while
 * it is the current one, but the fields telling where it belongs may arrive after it, so its
 * content has to be kept somewhere until then.
 *
 * The returned file is owned by the caller, which must delete it once the upload has been written
 * to its final location or discarded.
 *
 * @return The path to the staged file.
 */
suspend fun stageToTempFile(content: ByteReadChannel): Path = withContext(Dispatchers.IO) {
    val stagedFile = Files.createTempFile("hestia-upload-", ".part")

    try {
        Files.newOutputStream(stagedFile).use { output ->
            content.copyTo(output)
        }
        stagedFile
    } catch (e: Throwable) {
        Files.deleteIfExists(stagedFile)
        throw e
    }
}
