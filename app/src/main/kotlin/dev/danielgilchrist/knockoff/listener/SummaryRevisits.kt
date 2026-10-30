package dev.danielgilchrist.knockoff.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class SummaryRevisits(
    scope: CoroutineScope,
    private val revisit: suspend (List<PendingRevisit>) -> Unit,
) {
    companion object {
        private val QUIET = 500.milliseconds
    }

    private val pending = Channel<PendingRevisit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (first in pending) {
                delay(QUIET)
                revisit(listOf(first) + drained())
            }
        }
    }

    fun add(revisit: PendingRevisit) {
        pending.trySend(revisit)
    }

    private fun drained(): List<PendingRevisit> = generateSequence { pending.tryReceive().getOrNull() }.toList()
}
