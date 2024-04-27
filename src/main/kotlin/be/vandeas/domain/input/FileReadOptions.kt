package be.vandeas.domain.input

import kotlinx.serialization.Serializable

@Serializable
data class FileReadOptions(
    val path: String,
    val fileName: String,
)
