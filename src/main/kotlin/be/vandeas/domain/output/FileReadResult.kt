package be.vandeas.domain.output

import java.nio.file.Path

sealed interface FileReadResult {
    data class Success(val data: ByteArray) : FileReadResult
    data class NotFound(val path: Path) : FileReadResult
    data class Failure(val message: String) : FileReadResult
}
