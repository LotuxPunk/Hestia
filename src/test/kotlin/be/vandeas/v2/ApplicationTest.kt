package be.vandeas.v2

import be.vandeas.config.DEFAULT_MAX_UPLOAD_SIZE_BYTES
import be.vandeas.dto.Base64FileCreationOptions
import be.vandeas.dto.ReadFileBytesResult
import be.vandeas.plugins.configureKoin
import be.vandeas.plugins.configureRouting
import be.vandeas.plugins.configureSecurity
import be.vandeas.plugins.configureSerialization
import be.vandeas.plugins.configureStatus
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.delay
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.getPosixFilePermissions
import kotlin.io.path.inputStream
import kotlin.io.path.readBytes
import kotlin.io.path.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ApplicationTest {

    val apiKey = System.getenv("API_KEY") ?: throw IllegalStateException("API_KEY is not set")

    /**
     * Creates a temporary file of [sizeBytes] bytes, filled with a repeating pattern.
     */
    private fun createTempFile(sizeBytes: Long): File {
        val file = Files.createTempFile("hestia-test-upload-", ".bin").toFile()
        val chunk = ByteArray(1 shl 16) { (it % 251).toByte() }

        file.outputStream().use { output ->
            var written = 0L
            while (written < sizeBytes) {
                val length = minOf(chunk.size.toLong(), sizeBytes - written).toInt()
                output.write(chunk, 0, length)
                written += length
            }
        }

        return file
    }

    private fun Path.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")

        inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun HttpClient.getToken(lifeTime: Duration): String? {
        return this.get("/v2/auth/token?lifeTime=${lifeTime.inWholeSeconds}") {
            header("Authorization", apiKey)
            accept(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }.body<Map<String, String>>()["token"]
    }

    @Test
    fun `Should be able to write and read`() = testApplication {
        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }


        val jwt = httpClient.getToken(60.seconds)!!
        val dirName = UUID.randomUUID().toString()
        val fileNames = listOf(
            "file.txt",
            "file.pdf",
            "img.webp"
        )

        fileNames.forEach { fileName ->
            val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

            httpClient.post("/v2/file") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(jwt)
                setBody(
                    Base64FileCreationOptions(
                        path = dirName,
                        fileName = fileName,
                        content = testedFile.readBytes().encodeBase64()
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                assertEquals(mapOf("path" to dirName, "fileName" to fileName), body())
            }

            httpClient.get("/v2/file?path=$dirName&fileName=$fileName") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(
                    testedFile.readBytes().toList(),
                    body<ReadFileBytesResult>().content.decodeBase64Bytes().toList()
                )
            }
        }
    }

    @Test
    fun `Should not be able to use a token once expired`() = testApplication {
        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
            configureStatus()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val dirName = UUID.randomUUID().toString()
        val fileName = "file.txt"
        val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

        val token = httpClient.getToken(5.seconds)!!

        httpClient.post("/v2/file") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(
                Base64FileCreationOptions(
                    path = dirName,
                    fileName = fileName,
                    content = testedFile.readBytes().encodeBase64()
                )
            )
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals(mapOf("path" to dirName, "fileName" to fileName), body())
        }

        delay(6.seconds)

        httpClient.get("/v2/file?path=$dirName&fileName=$fileName") {
            bearerAuth(token)
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `Should be able to delete a file`() = testApplication {
        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val dirName = UUID.randomUUID().toString()
        val fileName = "file.txt"
        val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

        val jwt = httpClient.getToken(60.seconds)!!

        httpClient.post("/v2/file") {
            contentType(ContentType.Application.Json)
            bearerAuth(jwt)
            setBody(
                Base64FileCreationOptions(
                    path = dirName,
                    fileName = fileName,
                    content = testedFile.readBytes().encodeBase64()
                )
            )
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals(mapOf("path" to dirName, "fileName" to fileName), body())
        }

        httpClient.delete("/v2/file?path=$dirName&fileName=$fileName") {
            bearerAuth(jwt)
        }.apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }

        httpClient.get("/v2/file?path=$dirName&fileName=$fileName") {
            bearerAuth(jwt)
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `Should be able to upload in multipart-form data`() = testApplication {
        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val fileNames = mapOf(
            "file.txt" to ContentType.Text.Plain,
            "file.pdf" to ContentType.Application.Pdf,
            "img.webp" to ContentType.Image.Any
        )

        val jwt = httpClient.getToken(60.seconds)!!
        val dirName = "multipart-${UUID.randomUUID()}"

        fileNames.forEach { (fileName, contentType) ->
            val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

            httpClient.submitFormWithBinaryData("/v2/file/upload", formData {
                append(key = "path", value = dirName)
                append(key = "fileName", value = fileName)
                append(key = "content", value = testedFile.readBytes(), headers = Headers.build {
                    append(HttpHeaders.ContentType, contentType.toString())
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }) {
                bearerAuth(jwt)
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                assertEquals(mapOf("path" to dirName, "fileName" to fileName), body())
            }

            httpClient.get("/v2/file?path=$dirName&fileName=$fileName") {
                bearerAuth(jwt)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(
                    testedFile.readBytes().toList(),
                    body<ReadFileBytesResult>().content.decodeBase64Bytes().toList()
                )
            }
        }
    }

    @Test
    fun `Should be able to upload public file in multipart-form data`() = testApplication {
        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val fileNames = mapOf(
            "file.txt" to ContentType.Text.Plain,
            "file.pdf" to ContentType.Application.Pdf,
            "img.webp" to ContentType.Image.Any
        )

        val jwt = httpClient.getToken(60.seconds)!!
        val dirName = "multipart-${UUID.randomUUID()}"

        fileNames.forEach { (fileName, contentType) ->
            val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

            httpClient.submitFormWithBinaryData("/v2/file/upload", formData {
                append(key = "path", value = dirName)
                append(key = "public", value = true)
                append(key = "fileName", value = fileName)
                append(key = "content", value = testedFile.readBytes(), headers = Headers.build {
                    append(HttpHeaders.ContentType, contentType.toString())
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }) {
                bearerAuth(jwt)
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                val response = body<Map<String, String>>()

                assertEquals(mapOf("path" to listOf("public", dirName).joinToString("/"), "fileName" to fileName), response)

                httpClient.get("/v2/file/${response["path"]}/${response["fileName"]}") {
                    accept(ContentType.Application.OctetStream)
                    accept(ContentType.Text.Plain)
                    accept(ContentType.Application.Pdf)
                    accept(ContentType.Image.Any)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(
                        testedFile.readBytes().toList(),
                        bodyAsChannel().toInputStream().readBytes().toList()
                    )
                }
            }
        }
    }

    @Test
    fun `Should be able to upload public file in multipart-form data and delete it`() = testApplication {
        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val fileNames = mapOf("file.txt" to ContentType.Text.Plain,)

        val jwt = httpClient.getToken(60.seconds)!!
        val dirName = "multipart-${UUID.randomUUID()}"

        fileNames.forEach { (fileName, contentType) ->
            val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

            httpClient.submitFormWithBinaryData("/v2/file/upload", formData {
                append(key = "path", value = dirName)
                append(key = "public", value = true)
                append(key = "fileName", value = fileName)
                append(key = "content", value = testedFile.readBytes(), headers = Headers.build {
                    append(HttpHeaders.ContentType, contentType.toString())
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }) {
                bearerAuth(jwt)
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                val response = body<Map<String, String>>()

                assertEquals(mapOf("path" to listOf("public", dirName).joinToString("/"), "fileName" to fileName), response)

                httpClient.get("/v2/file/${response["path"]}/${response["fileName"]}") {
                    accept(ContentType.Application.OctetStream)
                    accept(ContentType.Text.Plain)
                    accept(ContentType.Application.Pdf)
                    accept(ContentType.Image.Any)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(
                        testedFile.readBytes().toList(),
                        bodyAsChannel().toInputStream().readBytes().toList()
                    )
                }

                httpClient.delete("/v2/file?path=${dirName}&fileName=${response["fileName"]}&public=true") {
                    bearerAuth(jwt)
                }.apply {
                    assertEquals(HttpStatusCode.NoContent, status)
                }

                httpClient.get("/v2/file/${response["path"]}/${response["fileName"]}") {
                    accept(ContentType.Application.OctetStream)
                    accept(ContentType.Text.Plain)
                    accept(ContentType.Application.Pdf)
                    accept(ContentType.Image.Any)
                }.apply {
                    assertEquals(HttpStatusCode.NotFound, status)
                }
            }
        }
    }

    @Test
    fun `Should not be able to create a file outside of the base directory`() = testApplication {
        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val jwt = httpClient.getToken(60.seconds)!!
        val dirName = "../outside-${UUID.randomUUID()}"
        val fileName = "file.txt"
        val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

        httpClient.post("/v2/file") {
            contentType(ContentType.Application.Json)
            bearerAuth(jwt)
            setBody(
                Base64FileCreationOptions(
                    path = dirName,
                    fileName = fileName,
                    content = testedFile.readBytes().encodeBase64()
                )
            )
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `Should not be able to make any request outside of the base directory`() = testApplication {
        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val jwt = httpClient.getToken(60.seconds)!!
        val dirName = "../outside-${UUID.randomUUID()}"
        val fileName = "file.txt"
        val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

        httpClient.post("/v2/file") {
            contentType(ContentType.Application.Json)
            bearerAuth(jwt)
            setBody(
                Base64FileCreationOptions(
                    path = dirName,
                    fileName = fileName,
                    content = testedFile.readBytes().encodeBase64()
                )
            )
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }

        httpClient.get("/v2/file?path=$dirName&fileName=$fileName") {
            bearerAuth(jwt)
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }

        httpClient.delete("/v2/file?path=$dirName&fileName=$fileName") {
            bearerAuth(jwt)
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `Should be able to upload a file bigger than the default multipart limit`() = testApplication {
        environment {
            // Pinned, so that a MAX_UPLOAD_SIZE_BYTES set in the environment running the tests
            // cannot decide what this test asserts.
            config = MapApplicationConfig("hestia.upload.maxSizeBytes" to DEFAULT_MAX_UPLOAD_SIZE_BYTES.toString())
        }

        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
            configureStatus()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val jwt = httpClient.getToken(60.seconds)!!
        val dirName = "multipart-${UUID.randomUUID()}"
        // Over the 50 MiB Ktor applies to every part of a multipart request by default.
        val testedFile = createTempFile(sizeBytes = 56L * 1024 * 1024)
        // A part whose length is unknown, as a browser sends it, is read until the next boundary,
        // while one that declares its length is read up to it. Both are capped.
        val parts = mapOf(
            "big-unknown-length.bin" to ChannelProvider { testedFile.readChannel() },
            "big-declared-length.bin" to ChannelProvider(testedFile.length()) { testedFile.readChannel() }
        )

        try {
            parts.forEach { (fileName, content) ->
                httpClient.submitFormWithBinaryData("/v2/file/upload", formData {
                    append(key = "path", value = dirName)
                    append(key = "fileName", value = fileName)
                    append(
                        key = "content",
                        value = content,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                }) {
                    bearerAuth(jwt)
                }.apply {
                    assertEquals(HttpStatusCode.Created, status)
                    assertEquals(mapOf("path" to dirName, "fileName" to fileName), body())
                }

                val uploadedFile = Path(System.getenv("BASE_DIRECTORY"), dirName, fileName)

                assertEquals(testedFile.length(), uploadedFile.fileSize())
                assertEquals(testedFile.toPath().sha256(), uploadedFile.sha256())
                // Staged uploads are moved into place, they must not keep the permissions of the
                // temporary file they were streamed to.
                assertEquals("rw-r--r--", PosixFilePermissions.toString(uploadedFile.getPosixFilePermissions()))
            }
        } finally {
            testedFile.delete()
        }
    }

    @Test
    fun `Should not be able to overwrite an existing file with an upload`() = testApplication {
        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
            configureStatus()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val jwt = httpClient.getToken(60.seconds)!!
        val dirName = "multipart-${UUID.randomUUID()}"
        val fileName = "file.txt"
        val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()
        val partHeaders = Headers.build {
            append(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
        }

        suspend fun upload(content: ByteArray) = httpClient.submitFormWithBinaryData("/v2/file/upload", formData {
            append(key = "path", value = dirName)
            append(key = "fileName", value = fileName)
            append(key = "content", value = content, headers = partHeaders)
        }) {
            bearerAuth(jwt)
        }

        upload(testedFile.readBytes()).apply {
            assertEquals(HttpStatusCode.Created, status)
        }

        upload("overwritten".toByteArray()).apply {
            assertEquals(HttpStatusCode.Conflict, status)
        }

        assertEquals(
            testedFile.readBytes().toList(),
            Path(System.getenv("BASE_DIRECTORY"), dirName, fileName).readBytes().toList()
        )
    }

    @Test
    fun `Should not be able to upload a file bigger than the configured limit`() = testApplication {
        environment {
            // Keeps the test cheap, instead of uploading more than the configured maximum size.
            config = MapApplicationConfig("hestia.upload.maxSizeBytes" to "1024")
        }

        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
            configureStatus()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val jwt = httpClient.getToken(60.seconds)!!
        val dirName = "multipart-${UUID.randomUUID()}"
        val fileName = "over-limit.bin"
        val testedFile = createTempFile(sizeBytes = 4L * 1024)
        val partHeaders = Headers.build {
            append(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
        }

        try {
            // A part of unknown length is rejected while the parser looks for the next boundary.
            httpClient.submitFormWithBinaryData("/v2/file/upload", formData {
                append(key = "path", value = dirName)
                append(key = "fileName", value = fileName)
                append(key = "content", value = ChannelProvider { testedFile.readChannel() }, headers = partHeaders)
            }) {
                bearerAuth(jwt)
            }.apply {
                assertEquals(HttpStatusCode.PayloadTooLarge, status)
            }

            // A part that declares its length is rejected on that length.
            httpClient.submitFormWithBinaryData("/v2/file/upload", formData {
                append(key = "path", value = dirName)
                append(key = "fileName", value = fileName)
                append(key = "content", value = testedFile.readBytes(), headers = partHeaders)
            }) {
                bearerAuth(jwt)
            }.apply {
                assertEquals(HttpStatusCode.PayloadTooLarge, status)
            }

            assertFalse(Path(System.getenv("BASE_DIRECTORY"), dirName, fileName).exists())
        } finally {
            testedFile.delete()
        }
    }

    @Test
    fun `Should only be able to embed a file with a valid token`() = testApplication {
        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
            configureStatus()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val jwt = httpClient.getToken(60.seconds)!!
        val dirName = UUID.randomUUID().toString()
        val fileName = "file.txt"
        val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

        httpClient.post("/v2/file") {
            contentType(ContentType.Application.Json)
            bearerAuth(jwt)
            setBody(
                Base64FileCreationOptions(
                    path = dirName,
                    fileName = fileName,
                    content = testedFile.readBytes().encodeBase64()
                )
            )
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
        }

        val invalidTokens = mapOf(
            "not a token at all" to "not-a-jwt",
            "an empty token" to "",
            "a token signed with another secret" to jwt.dropLast(4) + "AAAA"
        )

        invalidTokens.forEach { (description, token) ->
            httpClient.get("/v2/file/embed?path=$dirName&fileName=$fileName&token=$token").apply {
                assertEquals(HttpStatusCode.Unauthorized, status, "Embedded a file with $description")
            }
        }

        httpClient.get("/v2/file/embed?path=$dirName&fileName=$fileName&token=$jwt").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(testedFile.readBytes().toList(), bodyAsChannel().toInputStream().readBytes().toList())
        }
    }

    @Test
    fun `Should not be able to query a file outside of the public directory`() = testApplication {
        application {
            configureSerialization()
            configureKoin()
            configureSecurity()
            configureRouting()
        }

        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val jwt = httpClient.getToken(60.seconds)!!
        val fileName = "file.txt"

        httpClient.get("/v2/file/public/../input/file.pdf") {
            bearerAuth(jwt)
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }
}
