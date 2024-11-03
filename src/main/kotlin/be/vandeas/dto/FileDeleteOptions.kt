package be.vandeas.dto

import kotlinx.serialization.Serializable

@Serializable
data class FileDeleteOptions(
    override val path: String,
    override val fileName: String,
    override val public: Boolean,
): FileOperationOptions
