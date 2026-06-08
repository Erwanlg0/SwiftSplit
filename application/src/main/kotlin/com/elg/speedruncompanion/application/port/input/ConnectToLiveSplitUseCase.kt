package com.elg.speedruncompanion.application.port.input

interface ConnectToLiveSplitUseCase {
    suspend operator fun invoke(host: String, port: Int = 16834): Result<Unit>
}
