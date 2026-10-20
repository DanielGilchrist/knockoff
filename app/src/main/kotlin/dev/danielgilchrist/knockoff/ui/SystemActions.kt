package dev.danielgilchrist.knockoff.ui

data class SystemActions(
    val onOpenAccessSettings: () -> Unit,
    val onLinkDevice: () -> Unit,
    val onAllowNotifications: () -> Unit,
)
