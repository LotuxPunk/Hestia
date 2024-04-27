package be.vandeas.domain.output

import java.nio.file.Path

sealed interface FileCreationResult {
    data class Success(val path: Path) : FileCreationResult
    data class NotFound(val path: Path) : FileCreationResult
    data class Duplicate(val path: Path) : FileCreationResult
    data class Failure(val message: String) : FileCreationResult
}
