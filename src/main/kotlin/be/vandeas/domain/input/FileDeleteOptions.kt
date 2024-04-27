package be.vandeas.domain.input

import kotlinx.serialization.Serializable

@Serializable
data class FileDeleteOptions(
    val path: String,
    val fileName: String,
)
