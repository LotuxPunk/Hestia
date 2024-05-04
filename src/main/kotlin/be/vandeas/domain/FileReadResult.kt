package be.vandeas.domain

import java.io.File
import java.nio.file.Path

sealed interface FileReadResult {
    data class Success(val file: File) : FileReadResult
    data class NotFound(val path: Path) : FileReadResult
    data class Failure(val message: String) : FileReadResult
}
