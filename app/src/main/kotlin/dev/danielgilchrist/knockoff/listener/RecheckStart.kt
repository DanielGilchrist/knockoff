package dev.danielgilchrist.knockoff.listener

import kotlinx.coroutines.Job

internal sealed interface RecheckStart {
    data class Started(
        val job: Job,
    ) : RecheckStart

    data class Skipped(
        val detail: String,
    ) : RecheckStart
}
