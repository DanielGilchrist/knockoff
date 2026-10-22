package dev.danielgilchrist.knockoff.domain.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface StoredAppScopeV7 {
    fun upgraded(): StoredAppScope

    @Serializable
    @SerialName("everything")
    data object Everything : StoredAppScopeV7 {
        override fun upgraded(): StoredAppScope = StoredAppScope.Everything
    }

    @Serializable
    @SerialName("narrowed")
    data class Narrowed(
        private val accounts: List<StoredAccount>,
        private val chats: List<StoredChatPick>,
        private val patterns: List<StoredPatternPick>,
        private val asking: Boolean,
    ) : StoredAppScopeV7 {
        override fun upgraded(): StoredAppScope =
            StoredAppScope.Narrowed(accounts, emptyList(), chats, patterns, asking)
    }
}
