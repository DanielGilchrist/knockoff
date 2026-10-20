package dev.danielgilchrist.knockoff.ui

enum class NearbyAsk {
    Askable,
    Blocked,
    ;

    companion object {
        fun after(
            granted: Boolean,
            rationale: Boolean,
        ): NearbyAsk =
            when {
                granted || rationale -> Askable
                else -> Blocked
            }
    }
}
