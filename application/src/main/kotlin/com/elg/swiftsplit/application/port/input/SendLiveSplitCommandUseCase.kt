package com.elg.swiftsplit.application.port.input

interface SendLiveSplitCommandUseCase {
    suspend operator fun invoke(command: String): Result<String?>
}
