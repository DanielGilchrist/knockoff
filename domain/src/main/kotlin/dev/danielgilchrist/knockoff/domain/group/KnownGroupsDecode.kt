package dev.danielgilchrist.knockoff.domain.group

sealed interface KnownGroupsDecode {
    data class Decoded(
        val groups: KnownGroups,
    ) : KnownGroupsDecode

    sealed interface Failure : KnownGroupsDecode {
        val detail: String
    }

    data class Unreadable(
        override val detail: String,
    ) : Failure

    data class UnsupportedVersion(
        val version: Int,
    ) : Failure {
        override val detail: String get() = "Unsupported known groups version $version"
    }
}
