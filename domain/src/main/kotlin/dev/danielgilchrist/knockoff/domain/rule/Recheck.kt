package dev.danielgilchrist.knockoff.domain.rule

sealed interface Recheck {
    data object Unchanged : Recheck

    data class Resnooze(
        val snooze: Decision.Snooze,
    ) : Recheck

    data class Release(
        val leave: Decision.Leave,
    ) : Recheck

    fun describe(): String =
        when (this) {
            Unchanged -> "unchanged"
            is Resnooze -> "re-${snooze.describe()}"
            is Release -> "release now, ${leave.describe()}"
        }
}
