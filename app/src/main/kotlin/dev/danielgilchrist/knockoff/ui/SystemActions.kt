package dev.danielgilchrist.knockoff.ui

data class SystemActions(
    val onOpenAccessSettings: () -> Unit,
    val onAllowNotifications: () -> Unit,
    val onAllowNearbyDevices: () -> Unit,
    val onAllowCalendar: () -> Unit,
    val onOpenBluetoothSettings: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onLinkPaired: (PairedDevice) -> Unit,
    val onLinkNearby: () -> Unit,
)
