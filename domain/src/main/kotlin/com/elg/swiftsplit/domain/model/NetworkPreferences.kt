package com.elg.swiftsplit.domain.model

data class NetworkPreferences(
    val pollingDelayMs: Long = DEFAULT_POLLING_DELAY_MS,
    val networkTimeoutMs: Long = DEFAULT_NETWORK_TIMEOUT_MS
) {
    init {
        require(pollingDelayMs in MIN_POLLING_DELAY_MS..MAX_POLLING_DELAY_MS) {
            "pollingDelayMs must be between $MIN_POLLING_DELAY_MS and $MAX_POLLING_DELAY_MS"
        }
        require(networkTimeoutMs in MIN_NETWORK_TIMEOUT_MS..MAX_NETWORK_TIMEOUT_MS) {
            "networkTimeoutMs must be between $MIN_NETWORK_TIMEOUT_MS and $MAX_NETWORK_TIMEOUT_MS"
        }
    }

    companion object {
        const val DEFAULT_POLLING_DELAY_MS = 100L
        const val DEFAULT_NETWORK_TIMEOUT_MS = 2000L
        const val MIN_POLLING_DELAY_MS = 50L
        const val MAX_POLLING_DELAY_MS = 5000L
        const val MIN_NETWORK_TIMEOUT_MS = 1000L
        const val MAX_NETWORK_TIMEOUT_MS = 30000L

        val POLLING_DELAY_OPTIONS = listOf(50L, 100L, 250L, 500L, 1000L)
        val NETWORK_TIMEOUT_OPTIONS = listOf(3000L, 5000L, 10000L, 15000L, 30000L)

        val DEFAULT = NetworkPreferences()
    }
}
