package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName

data class InstalledApp(
    val packageName: PackageName,
    val label: String,
)
