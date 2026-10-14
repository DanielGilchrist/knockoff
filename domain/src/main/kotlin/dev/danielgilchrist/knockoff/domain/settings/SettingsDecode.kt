package dev.danielgilchrist.knockoff.domain.settings

sealed interface SettingsDecode {
    data class Decoded(
        val settings: Settings,
    ) : SettingsDecode

    sealed interface Failure : SettingsDecode {
        val detail: String
    }

    data class Unreadable(
        override val detail: String,
    ) : Failure

    data class UnsupportedVersion(
        val version: Int,
    ) : Failure {
        override val detail: String get() = "Unsupported settings version $version"
    }

    data class Invalid(
        override val detail: String,
    ) : Failure
}
