package dev.danielgilchrist.knockoff.domain.group

internal sealed interface StoredKnownGroupsDocument {
    fun toKnownGroups(): KnownGroupsDecode
}
