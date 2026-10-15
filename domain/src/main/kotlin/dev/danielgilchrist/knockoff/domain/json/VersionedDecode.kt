package dev.danielgilchrist.knockoff.domain.json

internal sealed interface VersionedDecode<out T> {
    data class Decoded<T>(
        val value: T,
    ) : VersionedDecode<T>

    data class Unreadable(
        val detail: String,
    ) : VersionedDecode<Nothing>

    data class UnsupportedVersion(
        val version: Int,
    ) : VersionedDecode<Nothing>
}
