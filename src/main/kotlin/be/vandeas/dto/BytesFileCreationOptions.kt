package be.vandeas.dto

import kotlinx.serialization.Serializable

@Serializable
data class BytesFileCreationOptions(
    val path: String,
    val fileName: String,
    val content: ByteArray,
)
