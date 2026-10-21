package dev.danielgilchrist.knockoff.ui.appicon

import dev.danielgilchrist.knockoff.domain.notification.PackageName

data class IconKey(
    val app: PackageName,
    val px: Int,
)
