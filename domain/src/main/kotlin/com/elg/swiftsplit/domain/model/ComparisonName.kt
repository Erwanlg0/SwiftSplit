package com.elg.swiftsplit.domain.model

@JvmInline
value class ComparisonName(val name: String) {
    companion object {
        val PERSONAL_BEST = ComparisonName("Personal Best")
        val BEST_SEGMENTS = ComparisonName("Best Segments")
        val AVERAGE_SEGMENTS = ComparisonName("Average Segments")
        val WORST_SEGMENTS = ComparisonName("Worst Segments")
        val MEDIAN_SEGMENTS = ComparisonName("Median Segments")
        val LATEST_RUN = ComparisonName("Latest Run")
    }
}
