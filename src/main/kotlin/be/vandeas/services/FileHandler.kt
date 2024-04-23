package be.vandeas.services

interface FileHandler {
    fun createDirectory(path: String)
    fun read(path: String): ByteArray
    fun write(path: String, content: ByteArray)
}
