package dev.danielgilchrist.knockoff.listener

import kotlinx.coroutines.Job

internal sealed interface Connection {
    data object Disconnected : Connection

    data class Connected(
        val sweeps: Job,
    ) : Connection

    fun stop() {
        when (this) {
            Disconnected -> {}

            is Connected -> {
                sweeps.cancel()
            }
        }
    }
}
