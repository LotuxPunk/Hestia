package be.vandeas.dto

import be.vandeas.domain.FileBytesReadResult
import io.ktor.util.*
import kotlinx.serialization.Serializable

@Serializable
data class ReadFileBytesResult(
    val content: String,
    val fileName: String
) {
    companion object {
        fun FileBytesReadResult.Success.mapToReadFileBytesDto(): ReadFileBytesResult = ReadFileBytesResult(
            content = this.data.encodeBase64(),
            fileName = this.filePath.fileName.toString()
        )
    }
}
