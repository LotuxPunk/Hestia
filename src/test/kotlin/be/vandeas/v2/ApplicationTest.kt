package be.vandeas.v2

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
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.delay
import java.util.*
import kotlin.io.path.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ApplicationTest {

    val apiKey = System.getenv("API_KEY") ?: throw IllegalStateException("API_KEY is not set")

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
