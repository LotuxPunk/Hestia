package be.vandeas.domain

import java.nio.file.Path

sealed interface FileBytesReadResult {
    data class Success(val data: ByteArray, val filePath: Path) : FileBytesReadResult
    data class NotFound(val path: Path) : FileBytesReadResult
    data class Failure(val message: String) : FileBytesReadResult
    data class BadRequest(val message: String) : FileBytesReadResult
}
