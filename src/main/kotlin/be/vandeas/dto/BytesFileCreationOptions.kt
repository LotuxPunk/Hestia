package be.vandeas.dto

import kotlinx.serialization.Serializable

@Serializable
data class BytesFileCreationOptions(
    override val path: String,
    override val fileName: String,
    override val public: Boolean = false,
    val content: ByteArray,
): FileOperationOptions
