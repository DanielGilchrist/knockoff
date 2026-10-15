package dev.danielgilchrist.knockoff.ui.zone

import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice

internal sealed interface ZoneSelection {
    data object None : ZoneSelection

    data class Chosen(
        val choice: ZoneChoice,
    ) : ZoneSelection
}
