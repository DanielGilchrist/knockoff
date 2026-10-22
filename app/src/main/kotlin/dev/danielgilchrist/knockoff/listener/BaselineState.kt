package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.change.Baseline

internal sealed interface BaselineState {
    data object NotLoaded : BaselineState

    data class Loaded(
        val baseline: Baseline<Applying>,
    ) : BaselineState
}
