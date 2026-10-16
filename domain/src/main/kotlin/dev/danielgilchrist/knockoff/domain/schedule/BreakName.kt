package dev.danielgilchrist.knockoff.domain.schedule

@JvmInline
value class BreakName private constructor(
    val value: String,
) {
    companion object {
        fun parse(raw: String): BreakNameParse =
            when {
                raw.isBlank() -> BreakNameParse.Blank
                else -> BreakNameParse.Valid(BreakName(raw.trim()))
            }
    }
}
