package dev.danielgilchrist.knockoff.domain.group

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredKnownGroupsV1(
    private val version: Int,
    private val apps: List<StoredKnownAppV1>,
) : StoredKnownGroupsDocument {
    companion object {
        const val VERSION = 1
    }

    override fun toKnownGroups(): KnownGroupsDecode =
        StoredKnownGroups(StoredKnownGroups.CURRENT_VERSION, apps.map(StoredKnownAppV1::upgraded)).toKnownGroups()
}
