package be.vandeas

import be.vandeas.dto.FileCreationOptions
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

    private val client = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json()
        }
    }

    @Test
    fun testRoot() {
        runBlocking {
            val name = UUID.randomUUID().toString()
            val fileNames = listOf(
                "file.txt",
                "file.pdf",
                "img.webp"
            )

            fileNames.forEach { fileName ->
                val textFile = this::class.java.classLoader.getResource("input/$fileName")!!.toURI().toPath().toFile()

                client.post("http://localhost:8082/v1") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        FileCreationOptions(
                            path = name,
                            fileName = fileName,
                            content = textFile.readBytes().encodeBase64()
                        )
                    )
                }.apply {
                    assertEquals(HttpStatusCode.Created, status)
                    assertEquals(mapOf("path" to name, "fileName" to fileName) , body())
                }
            }
        }
    }
}
