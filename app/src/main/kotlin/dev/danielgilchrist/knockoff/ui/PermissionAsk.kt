package dev.danielgilchrist.knockoff.ui

enum class PermissionAsk {
    Askable,
    Blocked,
    ;

    companion object {
        fun after(
            granted: Boolean,
            rationale: Boolean,
        ): PermissionAsk =
            when {
                granted || rationale -> Askable
                else -> Blocked
            }
    }
}
