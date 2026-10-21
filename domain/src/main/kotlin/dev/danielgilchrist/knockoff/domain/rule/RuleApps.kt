package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.PackageName

@ConsistentCopyVisibility
data class RuleApps private constructor(
    val scopes: Map<PackageName, AppScope>,
) {
    companion object {
        val NONE: RuleApps get() = RuleApps(emptyMap())

        fun of(scopes: Map<PackageName, AppScope>): RuleApps = RuleApps(scopes)
    }

    val targets: Set<Target>
        get() =
            scopes
                .flatMap { (packageName, scope) ->
                    when (scope) {
                        AppScope.Everything -> {
                            listOf(Target.App(packageName))
                        }

                        is AppScope.Narrowed -> {
                            scope.accounts.map { (group, label) -> Target.Account(packageName, group, label) } +
                                scope.chats.values.map { it.inApp(packageName) } +
                                scope.patterns.map { it.inApp(packageName) }
                        }
                    }
                }.toSet()

    val asking: Set<PackageName>
        get() = scopes.keys.filter(::asks).toSet()

    fun asks(packageName: PackageName): Boolean =
        when (val scope = scopes[packageName]) {
            null, AppScope.Everything -> false
            is AppScope.Narrowed -> scope.asking == NewChatsAsking.Ask
        }

    fun accountsOf(packageName: PackageName): Map<ChannelGroupId, GroupName> =
        when (val scope = scopes[packageName]) {
            null, AppScope.Everything -> emptyMap()
            is AppScope.Narrowed -> scope.accounts
        }

    fun chatsOf(packageName: PackageName): Map<ConversationId, ChatPick> =
        when (val scope = scopes[packageName]) {
            null, AppScope.Everything -> emptyMap()
            is AppScope.Narrowed -> scope.chats
        }

    fun withChat(chat: Target.Chat): RuleApps =
        when (val scope = scopes[chat.packageName]) {
            AppScope.Everything -> {
                this
            }

            is AppScope.Narrowed -> {
                RuleApps(scopes + (chat.packageName to scope.withChat(ChatPick.of(chat))))
            }

            null -> {
                RuleApps(scopes + (chat.packageName to AppScope.Narrowed.of(ChatPick.of(chat))))
            }
        }

    fun rebased(
        opened: RuleApps,
        current: RuleApps,
    ): RuleApps {
        val gained = current.scopes.filterKeys { it !in opened.scopes && it !in scopes }

        return RuleApps(
            scopes.mapValues { (packageName, scope) ->
                when (scope) {
                    AppScope.Everything -> {
                        scope
                    }

                    is AppScope.Narrowed -> {
                        scope.withChats(
                            current.chatsOf(packageName) - opened.chatsOf(packageName).keys,
                        )
                    }
                }
            } + gained,
        )
    }

    internal fun expanded(seen: SeenChats): RuleApps =
        RuleApps(
            scopes.mapValues { (packageName, scope) ->
                when (scope) {
                    AppScope.Everything -> scope
                    is AppScope.Narrowed -> scope.withChats(seenFor(packageName, scope, seen))
                }
            },
        )

    private fun seenFor(
        packageName: PackageName,
        scope: AppScope.Narrowed,
        seen: SeenChats,
    ): Map<ConversationId, ChatPick> {
        val patterns = scope.patterns.map { it.inApp(packageName) }

        return seen.chats
            .filter { chat ->
                chat.packageName == packageName &&
                    (chat.id in scope.chats || patterns.any { it.expandsTo(chat) })
            }.associate { chat ->
                chat.id to
                    when (val picked = scope.chats[chat.id]) {
                        null -> ChatPick.of(chat)
                        else -> picked.copy(channel = chat.channel)
                    }
            }
    }
}
