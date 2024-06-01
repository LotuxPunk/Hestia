package be.vandeas.logic.impl

import be.vandeas.exception.AuthorizationException
import be.vandeas.logic.AuthLogic
import io.github.reactivecircus.cache4k.Cache
import java.nio.file.Path
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class AuthLogicImpl : AuthLogic {
    private val tokenCache = Cache.Builder<String, Path>().expireAfterWrite(5.minutes).build()
    private val apiKey: String = System.getenv("API_KEY") ?: throw IllegalStateException("API_KEY is not set")

    override fun getOneTimeToken(apiKey: String, path: String, fileName: String?): String {
        if (apiKey != this.apiKey) {
            throw AuthorizationException("Invalid API key")
        }

        return UUID.randomUUID().toString().also { token ->
            tokenCache.put(token, Path.of(path, fileName ?: ""))
        }
    }

    override fun validateOneTimeToken(token: String, path: Path): Path =
        tokenCache.get(token)
            ?.takeIf { it == path }?.also {
                tokenCache.invalidate(token)
            } ?: throw AuthorizationException("Invalid one-time token")

    override fun <T> guard(token: String, path: Path, protectedMethod: () -> T): T =
        validateOneTimeToken(token, path).let { protectedMethod() }

    override fun validateApiKey(apiKey: String): Boolean =
        apiKey == this.apiKey
}
