package dev.danielgilchrist.knockoff.domain.channel

enum class Importance {
    None,
    Min,
    Low,
    Default,
    High,
    Max,
    ;

    fun loudness(): Loudness =
        when (this) {
            None, Min, Low -> Loudness.Quiet
            Default -> Loudness.Alerting(AlertingImportance.Default)
            High -> Loudness.Alerting(AlertingImportance.High)
            Max -> Loudness.Alerting(AlertingImportance.Max)
        }
}
