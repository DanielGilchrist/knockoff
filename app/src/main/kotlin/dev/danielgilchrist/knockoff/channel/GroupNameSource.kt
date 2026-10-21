package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.notification.PackageName

internal fun interface GroupNameSource {
    fun of(packageName: PackageName): GroupNamesFetch
}
