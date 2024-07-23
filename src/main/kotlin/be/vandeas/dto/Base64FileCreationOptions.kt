package be.vandeas.dto

import kotlinx.serialization.Serializable

@Serializable
data class Base64FileCreationOptions(
    val path: String,
    val fileName: String,
    val content: String,
)
