package be.vandeas.v1

import be.vandeas.dto.Base64FileCreationOptions
import be.vandeas.dto.ReadFileBytesResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.io.path.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class ApplicationTest {

    val apiKey = System.getenv("API_KEY") ?: throw IllegalStateException("API_KEY is not set")

    private suspend fun HttpClient.getToken(dir: String, fileName: String): String? {
        return this.get("/v1/auth/token?path=$dir&fileName=$fileName") {
            header("Authorization", apiKey)
            accept(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }.body<Map<String, String>>()["token"]
    }

    @Test
    fun `Should be able to write and read`() = testApplication {
        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val dirName = UUID.randomUUID().toString()
        val fileNames = listOf(
            "file.txt",
            "file.pdf",
            "img.webp"
        )

        fileNames.forEach { fileName ->
            val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

            httpClient.post("/v1/file") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header("Authorization", httpClient.getToken(dirName, fileName)!!)
                setBody(
                    Base64FileCreationOptions(
                        path = dirName,
                        fileName = fileName,
                        content = testedFile.readBytes().encodeBase64()
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                assertEquals(mapOf("path" to dirName, "fileName" to fileName) , body())
            }

            httpClient.get("/v1/file?path=$dirName&fileName=$fileName") {
                header("Authorization", httpClient.getToken(dirName, fileName)!!)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(testedFile.readBytes().toList(), body<ReadFileBytesResult>().content.decodeBase64Bytes().toList())
            }
        }
    }

    @Test
    fun `Should not be able to re-use the same token twice`() = testApplication {
        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val dirName = UUID.randomUUID().toString()
        val fileName = "file.txt"
        val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

        val token = httpClient.getToken(dirName, fileName)

        httpClient.post("/v1/file") {
            contentType(ContentType.Application.Json)
            header("Authorization", token!!)
            setBody(
                Base64FileCreationOptions(
                    path = dirName,
                    fileName = fileName,
                    content = testedFile.readBytes().encodeBase64()
                )
            )
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals(mapOf("path" to dirName, "fileName" to fileName) , body())
        }

        httpClient.get("/v1/file?path=$dirName&fileName=$fileName") {
            header("Authorization", token!!)
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `Should be able to delete a file`() = testApplication {
        val httpClient = client.config {
            install(ContentNegotiation) {
                json()
            }
        }

        val dirName = UUID.randomUUID().toString()
        val fileName = "file.txt"
        val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

        httpClient.post("/v1/file") {
            contentType(ContentType.Application.Json)
            header("Authorization", httpClient.getToken(dirName, fileName)!!)
            setBody(
                Base64FileCreationOptions(
                    path = dirName,
                    fileName = fileName,
                    content = testedFile.readBytes().encodeBase64()
                )
            )
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            assertEquals(mapOf("path" to dirName, "fileName" to fileName) , body())
        }

        httpClient.delete("/v1/file?path=$dirName&fileName=$fileName") {
            header("Authorization", httpClient.getToken(dirName, fileName)!!)
        }.apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }

        httpClient.get("/v1/file?path=$dirName&fileName=$fileName") {
            header("Authorization", httpClient.getToken(dirName, fileName)!!)
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `Should be able to upload in multipart-form data`() = testApplication {
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

        val dirName = "multipart-${UUID.randomUUID()}"

        fileNames.forEach { (fileName, contentType) ->
            val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

            httpClient.submitFormWithBinaryData("/v1/file/upload", formData {
                append(key = "path", value = dirName)
                append(key = "fileName", value = fileName)
                append(key = "content", value = testedFile.readBytes(), headers = Headers.build {
                    append(HttpHeaders.ContentType, contentType.toString())
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }) {
                header("Authorization", httpClient.getToken(dirName, fileName)!!)
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                assertEquals(mapOf("path" to dirName, "fileName" to fileName) , body())
            }

            httpClient.get("/v1/file?path=$dirName&fileName=$fileName") {
                header("Authorization", httpClient.getToken(dirName, fileName)!!)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(testedFile.readBytes().toList(), body<ReadFileBytesResult>().content.decodeBase64Bytes().toList())
            }
        }
    }

}
