package com.elg.speedruncompanion.application.port.output

import com.elg.speedruncompanion.domain.model.Run

interface RunFileParser {
    suspend fun parse(content: ByteArray): Result<Run>
    fun supportedExtensions(): List<String>
}
