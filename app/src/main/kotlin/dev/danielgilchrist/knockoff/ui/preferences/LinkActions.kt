package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.ui.SystemActions

data class LinkActions(
    val system: SystemActions,
    val onBack: () -> Unit,
)
