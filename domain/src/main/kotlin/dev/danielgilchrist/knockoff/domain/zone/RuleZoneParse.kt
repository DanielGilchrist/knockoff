package dev.danielgilchrist.knockoff.domain.zone

sealed interface RuleZoneParse {
    data class Valid(
        val zone: RuleZone,
    ) : RuleZoneParse

    data class Unknown(
        val cause: ZoneChoiceParse.Unknown,
    ) : RuleZoneParse
}
