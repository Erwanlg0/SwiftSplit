package com.elg.swiftsplit.application.port.output

import com.elg.swiftsplit.domain.model.Run

interface RunFileExporter {
    suspend fun export(run: Run): ByteArray
}
