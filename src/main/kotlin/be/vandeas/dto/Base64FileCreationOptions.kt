package be.vandeas.dto

import kotlinx.serialization.Serializable

@Serializable
data class Base64FileCreationOptions(
    override val path: String,
    override val fileName: String,
    override val public: Boolean = false,
    val content: String,
): FileOperationOptions
