package dev.danielgilchrist.knockoff.domain.group

@JvmInline
value class GroupName private constructor(
    val value: String,
) {
    companion object {
        fun parse(raw: String): GroupNameParse =
            when {
                raw.isBlank() -> GroupNameParse.Blank
                else -> GroupNameParse.Valid(GroupName(raw.trim()))
            }
    }
}
