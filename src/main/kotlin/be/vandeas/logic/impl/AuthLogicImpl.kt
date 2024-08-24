package be.vandeas.logic.impl

import be.vandeas.exception.AuthorizationException
import be.vandeas.logic.AuthLogic
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.reactivecircus.cache4k.Cache
import io.ktor.server.application.*
import java.nio.file.Path
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class AuthLogicImpl(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
    private val realm: String,
) : AuthLogic {
    private val tokenCache = Cache.Builder<String, Path>().expireAfterWrite(5.minutes).build()
    private val apiKey: String = System.getenv("API_KEY") ?: throw IllegalStateException("API_KEY is not set")

    @Deprecated("Use JWT tokens instead")
    override fun getOneTimeToken(apiKey: String, path: String, fileName: String?): String {
        if (!validateApiKey(apiKey)) {
            throw AuthorizationException("Invalid API key")
        }

        return UUID.randomUUID().toString().also { token ->
            tokenCache.put(token, Path.of(path, fileName ?: ""))
        }
    }

    @Deprecated("Use JWT tokens instead")
    override fun validateOneTimeToken(token: String, path: Path): Path =
        tokenCache.get(token)
            ?.takeIf { it == path }?.also {
                tokenCache.invalidate(token)
            } ?: throw AuthorizationException("Invalid one-time token")

    @Deprecated("Use JWT tokens instead")
    override fun <T> guard(token: String, path: Path, protectedMethod: () -> T): T =
        validateOneTimeToken(token, path).let { protectedMethod() }

    override fun <T> guard(jwt: String, protectedMethod: () -> T): T {
        TODO("Not yet implemented")
    }

    override fun getJwtToken(apiKey: String, duration: Duration): String {
        if (validateApiKey(apiKey)) {
            throw AuthorizationException("Invalid API key")
        }

        val token = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withExpiresAt(Date(System.currentTimeMillis() + duration.inWholeMilliseconds))
            .sign(Algorithm.HMAC256(secret))

        return token
    }

    override fun validateJwtToken(token: String): Boolean {
        runCatching {
            JWT.require(Algorithm.HMAC256(secret))
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
                .verify(token)
        }.onFailure {
            return false
        }
        return true
    }

    override fun validateApiKey(apiKey: String): Boolean =
        apiKey == this.apiKey
}
