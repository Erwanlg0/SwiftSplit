package com.elg.speedruncompanion.application.port.output

import com.elg.speedruncompanion.domain.model.Run

interface RunFileExporter {
    suspend fun export(run: Run): ByteArray
}
