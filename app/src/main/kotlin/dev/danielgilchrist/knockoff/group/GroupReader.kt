package dev.danielgilchrist.knockoff.group

import dev.danielgilchrist.knockoff.domain.notification.PackageName

fun interface GroupReader {
    suspend fun discover(packageName: PackageName): GroupDiscovery
}
