package dev.danielgilchrist.knockoff.domain.channel

enum class AlertingImportance(
    val importance: Importance,
) {
    Default(Importance.Default),
    High(Importance.High),
    Max(Importance.Max),
}
