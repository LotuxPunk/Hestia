package be.vandeas.config

import io.ktor.server.application.*

/**
 * Maximum upload size used when nothing is configured: 1 GiB.
 */
const val DEFAULT_MAX_UPLOAD_SIZE_BYTES: Long = 1024L * 1024 * 1024

/**
 * Maximum size, in bytes, accepted for a single part of a `multipart/form-data` upload.
 *
 * Ktor caps every part at 50 MiB by default, file parts included, which makes bigger uploads
 * fail with an [java.io.IOException] while the parser scans for the part boundary. This value
 * replaces that default and is read from the `hestia.upload.maxSizeBytes` configuration key,
 * i.e. from the `MAX_UPLOAD_SIZE_BYTES` environment variable.
 */
val ApplicationEnvironment.maxUploadSizeBytes: Long
    get() = config.propertyOrNull("hestia.upload.maxSizeBytes")
        ?.getString()
        ?.toLongOrNull()
        ?: DEFAULT_MAX_UPLOAD_SIZE_BYTES
