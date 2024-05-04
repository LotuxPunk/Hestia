package be.vandeas.domain

import java.nio.file.Path

sealed interface FileBytesReadResult {
    data class Success(val data: ByteArray) : FileBytesReadResult
    data class NotFound(val path: Path) : FileBytesReadResult
    data class Failure(val message: String) : FileBytesReadResult
}
