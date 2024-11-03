package be.vandeas.dto

import kotlinx.serialization.Serializable

@Serializable
data class DirectoryDeleteOptions(
    val path: String,
    val recursive: Boolean = false,
    override val public: Boolean,
): FileVisibilityOptions
