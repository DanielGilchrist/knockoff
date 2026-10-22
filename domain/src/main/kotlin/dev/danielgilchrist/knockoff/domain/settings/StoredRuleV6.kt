package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredRuleV6(
    private val name: String,
    private val state: String,
    private val policy: String,
    private val windows: List<StoredWindow>,
    private val breaks: List<StoredBreak>,
    private val apps: List<String>,
    private val gmailAccounts: List<String>,
    private val chats: List<StoredChat>,
    private val patterns: List<StoredPattern>,
    private val zone: String,
    private val asking: List<String>,
) {
    companion object {
        private const val GMAIL_ACCOUNT_GROUP = "^ncg_1_account_"
    }

    fun upgraded(): StoredRuleV7 =
        StoredRuleV7(
            name,
            state,
            policy,
            windows,
            breaks,
            zone,
            (apps + gmailApp() + chats.map { it.app } + patterns.map { it.app } + asking)
                .distinct()
                .map { app ->
                    when (app) {
                        in apps -> StoredAppV7(app, StoredAppScopeV7.Everything)
                        else -> StoredAppV7(app, narrowed(app))
                    }
                },
        )

    private fun gmailApp(): List<String> =
        when {
            gmailAccounts.isEmpty() -> emptyList()
            else -> listOf(PackageName.GMAIL.value)
        }

    private fun narrowed(app: String): StoredAppScopeV7.Narrowed =
        StoredAppScopeV7.Narrowed(
            accounts =
                when (app) {
                    PackageName.GMAIL.value -> gmailAccounts.map { StoredAccount(GMAIL_ACCOUNT_GROUP + it, it) }
                    else -> emptyList()
                },
            chats = chats.filter { it.app == app }.map { it.pick() },
            patterns = patterns.filter { it.app == app }.map { it.pick() },
            asking = app in asking,
        )
}
