package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.notification.GroupRole

internal sealed interface GroupRoleLookup {
    data class Found(
        val role: GroupRole,
    ) : GroupRoleLookup

    data class Unknown(
        val detail: String,
    ) : GroupRoleLookup
}
