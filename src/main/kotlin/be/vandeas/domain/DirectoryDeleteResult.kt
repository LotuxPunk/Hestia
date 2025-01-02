package be.vandeas.domain

import java.nio.file.Path

sealed interface DirectoryDeleteResult {
    data class Success(val path: Path) : DirectoryDeleteResult
    data class IsAFile(val path: Path) : DirectoryDeleteResult
    data class DirectoryHasChildren(val path: Path) : DirectoryDeleteResult
    data class NotFound(val path: Path) : DirectoryDeleteResult
    data class Failure(val message: String) : DirectoryDeleteResult
    data class BadRequest(val message: String) : DirectoryDeleteResult
}
