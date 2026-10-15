package dev.danielgilchrist.knockoff.listener

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin

internal sealed interface Connection {
    data object Idle : Connection

    data class Connected(
        val collector: Job,
    ) : Connection

    data class Disconnected(
        val collector: Job,
    ) : Connection

    fun disconnected(): Connection =
        when (this) {
            Idle -> {
                Idle
            }

            is Connected -> {
                collector.cancel()
                Disconnected(collector)
            }

            is Disconnected -> {
                this
            }
        }

    suspend fun join() {
        when (this) {
            Idle -> {}

            is Connected -> {
                collector.cancelAndJoin()
            }

            is Disconnected -> {
                collector.join()
            }
        }
    }
}
