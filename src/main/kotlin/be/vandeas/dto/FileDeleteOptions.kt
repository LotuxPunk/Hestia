package be.vandeas.dto

import kotlinx.serialization.Serializable

@Serializable
data class FileDeleteOptions(
    val path: String,
    val fileName: String,
)
