package be.vandeas

import be.vandeas.dto.FileCreationOptions
import be.vandeas.dto.ReadFileBytesResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.io.path.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class ApplicationTest {

    val apiKey = System.getenv("API_KEY") ?: throw IllegalStateException("API_KEY is not set")

    private val client = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json()
        }
    }

    private suspend fun getToken(dir: String, fileName: String): String?{
        return client.get("http://localhost:8082/v1/auth/token?path=$dir&fileName=$fileName") {
            header("Authorization", apiKey)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }.body<Map<String, String>>()["token"]
    }

    @Test
    fun `Should be able to write and read`() {
        runBlocking {
            val dirName = UUID.randomUUID().toString()
            val fileNames = listOf(
                "file.txt",
                "file.pdf",
                "img.webp"
            )

            fileNames.forEach { fileName ->
                val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

                client.post("http://localhost:8082/v1/file") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    header("Authorization", getToken(dirName, fileName)!!)
                    setBody(
                        FileCreationOptions(
                            path = dirName,
                            fileName = fileName,
                            content = testedFile.readBytes().encodeBase64()
                        )
                    )
                }.apply {
                    assertEquals(HttpStatusCode.Created, status)
                    assertEquals(mapOf("path" to dirName, "fileName" to fileName) , body())
                }

                client.get("http://localhost:8082/v1/file?path=$dirName&fileName=$fileName") {
                    header("Authorization", getToken(dirName, fileName)!!)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(testedFile.readBytes().toList(), body<ReadFileBytesResult>().content.decodeBase64Bytes().toList())
                }
            }
        }
    }

    @Test
    fun `Should not be able to re-use the same token twice`() {
        runBlocking {
            val dirName = UUID.randomUUID().toString()
            val fileName = "file.txt"
            val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

            val token = getToken(dirName, fileName)

            client.post("http://localhost:8082/v1/file") {
                contentType(ContentType.Application.Json)
                header("Authorization", token!!)
                setBody(
                    FileCreationOptions(
                        path = dirName,
                        fileName = fileName,
                        content = testedFile.readBytes().encodeBase64()
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                assertEquals(mapOf("path" to dirName, "fileName" to fileName) , body())
            }

            client.get("http://localhost:8082/v1/file?path=$dirName&fileName=$fileName") {
                header("Authorization", token!!)
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
        }
    }

    @Test
    fun `Should be able to delete a file`() {
        runBlocking {
            val dirName = UUID.randomUUID().toString()
            val fileName = "file.txt"
            val testedFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

            client.post("http://localhost:8082/v1/file") {
                contentType(ContentType.Application.Json)
                header("Authorization", getToken(dirName, fileName)!!)
                setBody(
                    FileCreationOptions(
                        path = dirName,
                        fileName = fileName,
                        content = testedFile.readBytes().encodeBase64()
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                assertEquals(mapOf("path" to dirName, "fileName" to fileName) , body())
            }

            client.delete("http://localhost:8082/v1/file?path=$dirName&fileName=$fileName") {
                header("Authorization", getToken(dirName, fileName)!!)
            }.apply {
                assertEquals(HttpStatusCode.NoContent, status)
            }

            client.get("http://localhost:8082/v1/file?path=$dirName&fileName=$fileName") {
                header("Authorization", getToken(dirName, fileName)!!)
            }.apply {
                assertEquals(HttpStatusCode.NotFound, status)
            }
        }
    }

}
