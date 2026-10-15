package dev.danielgilchrist.knockoff.domain.zone

sealed interface ZoneChoiceParse {
    data class Valid(
        val choice: ZoneChoice,
    ) : ZoneChoiceParse

    data class Unknown(
        val raw: String,
        val detail: String,
    ) : ZoneChoiceParse
}
