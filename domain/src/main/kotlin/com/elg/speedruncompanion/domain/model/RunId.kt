package com.elg.speedruncompanion.domain.model

import java.util.UUID

@JvmInline
value class RunId(val value: String) {
    companion object {
        fun generate(): RunId = RunId(UUID.randomUUID().toString())
    }
}
