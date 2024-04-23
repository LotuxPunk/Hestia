package be.vandeas

import kotlinx.serialization.Serializable

@Serializable
data class FileCreationResult(
    val path: String,
    val fileName: String,
    val success: Boolean
)
