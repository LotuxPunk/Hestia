package be.vandeas.config

import io.ktor.server.application.*

/**
 * Maximum upload size used when nothing is configured: 512 MiB.
 */
const val DEFAULT_MAX_UPLOAD_SIZE_BYTES: Long = 512L * 1024 * 1024

/**
 * Maximum size, in bytes, accepted for a single part of a `multipart/form-data` upload.
 *
 * Ktor caps every part at 50 MiB by default, file parts included, which makes bigger uploads
 * fail with an [java.io.IOException] while the parser scans for the part boundary. This value
 * replaces that default.
 *
 * It comes from the `MAX_UPLOAD_SIZE_BYTES` environment variable, as the rest of the configuration
 * of this service does: the server is started from code, so no configuration file is loaded. The
 * `hestia.upload.maxSizeBytes` configuration key takes precedence when present, which the tests use
 * and which would keep this working were the server ever started from a configuration file.
 */
val ApplicationEnvironment.maxUploadSizeBytes: Long
    get() = config.propertyOrNull("hestia.upload.maxSizeBytes")?.getString()?.toLongOrNull()
        ?: System.getenv("MAX_UPLOAD_SIZE_BYTES")?.toLongOrNull()
        ?: DEFAULT_MAX_UPLOAD_SIZE_BYTES
