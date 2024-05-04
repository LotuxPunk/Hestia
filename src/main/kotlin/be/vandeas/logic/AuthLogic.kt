package be.vandeas.logic

import java.nio.file.InvalidPathException
import java.nio.file.Path

interface AuthLogic {

    /**
     * Validates an API key.
     *
     * @return true if the API key is valid, false otherwise.
     */
    fun validateApiKey(apiKey: String): Boolean

    /**
     * Generates a one-time token for the given path and file name.
     *
     * @throws InvalidPathException if the path is invalid.
     */
    fun getOneTimeToken(apiKey: String, path: String, fileName: String?): String

    /**
     * Validates a one-time token.
     *
     * @return The path if the token is valid, null otherwise.
     */
    fun validateOneTimeToken(token: String, path: Path): Path?

    /**
     * Guards a protected method with a one-time token.
     *
     * @return The result of the protected method.
     */
    fun <T> guard(token: String, path: Path, protectedMethod: () -> T): T
}
