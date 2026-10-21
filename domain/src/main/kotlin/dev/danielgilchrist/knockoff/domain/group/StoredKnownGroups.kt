package dev.danielgilchrist.knockoff.domain.group

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredKnownGroups(
    private val version: Int,
    private val apps: List<StoredKnownApp>,
) {
    companion object {
        const val CURRENT_VERSION = 1

        fun from(groups: KnownGroups): StoredKnownGroups =
            StoredKnownGroups(CURRENT_VERSION, groups.apps.map(StoredKnownApp::from))
    }

    fun toKnownGroups(): KnownGroupsDecode = KnownGroupsDecode.Decoded(KnownGroups.of(apps.map { it.toKnownApp() }))
}
