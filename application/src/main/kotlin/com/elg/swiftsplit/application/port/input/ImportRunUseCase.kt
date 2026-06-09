package com.elg.swiftsplit.application.port.input

import com.elg.swiftsplit.domain.model.Run

interface ImportRunUseCase {
    suspend operator fun invoke(content: ByteArray): Result<Run>
}
