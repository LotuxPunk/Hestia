package be.vandeas.logic

import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.time.Duration

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
    @Deprecated("Use JWT tokens instead")
    fun getOneTimeToken(apiKey: String, path: String, fileName: String?): String

    /**
     * Validates a one-time token.
     *
     * @return The path if the token is valid, null otherwise.
     */
    @Deprecated("Use JWT tokens instead")
    fun validateOneTimeToken(token: String, path: Path): Path?

    /**
     * Guards a protected method with a one-time token.
     *
     * @return The result of the protected method.
     */
    @Deprecated("Use JWT tokens instead")
    fun <T> guard(token: String, path: Path, protectedMethod: () -> T): T

    /**
     * Guards a protected method with a JWT token.
     *
     * @return The result of the protected method.
     */
    fun <T> guard(jwt: String, protectedMethod: () -> T): T

    /**
     * Generates a JWT token for the given API key.
     *
     * @return The JWT token.
     */
    fun getJwtToken(apiKey: String, duration: Duration): String

    /**
     * Validates a JWT token.
     *
     * @return true if the token is valid, false otherwise.
     */
    fun validateJwtToken(token: String): Boolean
}
