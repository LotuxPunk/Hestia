package be.vandeas.dto

import kotlinx.serialization.Serializable

@Serializable
data class FileNameWithPath(
    val fileName: String,
    val path: String
)
