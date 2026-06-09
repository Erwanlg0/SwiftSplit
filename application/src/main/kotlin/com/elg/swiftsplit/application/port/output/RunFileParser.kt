package com.elg.swiftsplit.application.port.output

import com.elg.swiftsplit.domain.model.Run

interface RunFileParser {
    suspend fun parse(content: ByteArray): Result<Run>
    fun supportedExtensions(): List<String>
}
