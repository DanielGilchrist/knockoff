package dev.danielgilchrist.knockoff.domain.channel

sealed interface Loudness {
    data class Alerting(
        val importance: AlertingImportance,
    ) : Loudness

    data object Quiet : Loudness
}
