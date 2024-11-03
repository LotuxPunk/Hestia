package be.vandeas.dto

interface FileOperationOptions: FileVisibilityOptions {
    val path: String
    val fileName: String
}
