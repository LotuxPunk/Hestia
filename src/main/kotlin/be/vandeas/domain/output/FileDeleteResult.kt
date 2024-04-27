package be.vandeas.domain.output

import java.nio.file.Path

sealed interface FileDeleteResult {
    data class Success(val path: Path) : FileDeleteResult
    data class IsADirectory(val path: Path) : FileDeleteResult
    data class Failure(val message: String) : FileDeleteResult
    data class NotFound(val path: Path) : FileDeleteResult
}
