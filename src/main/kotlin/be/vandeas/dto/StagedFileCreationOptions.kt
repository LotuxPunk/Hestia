package be.vandeas.dto

import java.nio.file.Path

/**
 * Creation of a file whose content has already been staged on disk,
 * see [be.vandeas.handler.stageToTempFile].
 */
data class StagedFileCreationOptions(
    override val path: String,
    override val fileName: String,
    override val public: Boolean = false,
    val stagedFile: Path,
): FileOperationOptions
