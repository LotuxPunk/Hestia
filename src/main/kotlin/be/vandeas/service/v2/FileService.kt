package be.vandeas.service.v2

import be.vandeas.domain.*
import be.vandeas.dto.*

interface FileService {
    fun getFile(jwt: String, fileReadOptions: FileReadOptions): FileReadResult
}
