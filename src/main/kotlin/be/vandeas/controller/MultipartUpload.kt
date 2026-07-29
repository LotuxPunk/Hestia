package be.vandeas.controller

import be.vandeas.handler.stageToTempFile
import io.ktor.http.content.*
import java.nio.file.Files
import java.nio.file.Path

/**
 * A `multipart/form-data` upload whose content has been staged on disk.
 *
 * The [stagedFile] is owned by the caller, which must delete it once the upload has been written to
 * its final location or discarded.
 */
internal class StagedUpload(
    val path: String,
    val fileName: String,
    val public: Boolean,
    val stagedFile: Path,
)

/**
 * Reads a `multipart/form-data` upload, streaming the file part to a temporary file instead of
 * holding it in memory.
 *
 * @throws IllegalArgumentException When a required field is missing, or a part is not supported.
 */
internal suspend fun MultiPartData.stageUpload(): StagedUpload {
    var fileName: String? = null
    var path: String? = null
    var public = false
    var stagedFile: Path? = null

    try {
        forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    when (part.name) {
                        "path" -> path = part.value
                        "fileName" -> fileName = part.value
                        "public" -> public = part.value.toBoolean()
                    }
                }

                is PartData.FileItem -> {
                    stagedFile = stageToTempFile(part.provider())
                }

                else -> throw IllegalArgumentException("Unsupported part type: ${part::class.simpleName}")
            }
            part.dispose()
        }

        requireNotNull(fileName) { "fileName is required" }
        requireNotNull(path) { "path is required" }
        requireNotNull(stagedFile) { "data is required" }

        return StagedUpload(
            path = path!!,
            fileName = fileName!!,
            public = public,
            stagedFile = stagedFile!!
        )
    } catch (e: Throwable) {
        stagedFile?.let { Files.deleteIfExists(it) }
        throw e
    }
}
