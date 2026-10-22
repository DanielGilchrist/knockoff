package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.group.GroupDiscovery
import dev.danielgilchrist.knockoff.ui.AppDiscovery
import dev.danielgilchrist.knockoff.ui.DeviceLink
import dev.danielgilchrist.knockoff.ui.LinkState
import dev.danielgilchrist.knockoff.ui.possessive

sealed interface DiscoveryNote {
    data object None : DiscoveryNote

    data class Text(
        val text: String,
    ) : DiscoveryNote

    data class LinkNeeded(
        val title: String,
        val body: String,
    ) : DiscoveryNote

    companion object {
        fun of(
            facts: DiscoveryFacts,
            discovery: AppDiscovery,
            link: LinkState,
        ): DiscoveryNote =
            when {
                facts.accounts.naming == GroupNaming.SomeUnnamed && needsLink(link) -> unnamed(facts)
                facts.accounts.offered.isNotEmpty() -> None
                else -> unoffered(facts, discovery)
            }

        private fun unoffered(
            facts: DiscoveryFacts,
            discovery: AppDiscovery,
        ): DiscoveryNote =
            when (discovery) {
                AppDiscovery.NotAsked -> None
                AppDiscovery.Reading -> Text("Checking ${facts.label} for accounts and categories…")
                is AppDiscovery.Finished -> finished(facts, discovery.discovery)
            }

        private fun finished(
            facts: DiscoveryFacts,
            discovery: GroupDiscovery,
        ): DiscoveryNote =
            when (discovery) {
                GroupDiscovery.Read -> {
                    when (facts.kind) {
                        AppKind.Plain -> {
                            Text(
                                "${facts.label} has no accounts or categories to pick from. If it has chats, " +
                                    "they'll show here after it notifies you.",
                            )
                        }

                        AppKind.Accounts, AppKind.Chats, AppKind.AccountsAndChats -> {
                            None
                        }
                    }
                }

                is GroupDiscovery.NotLinked -> {
                    None
                }

                GroupDiscovery.Unbound -> {
                    Text(
                        "Knockoff couldn't look for ${possessive(
                            facts.label,
                        )} accounts and categories because it isn't " +
                            "connected to your notifications right now.",
                    )
                }

                is GroupDiscovery.Unrecorded -> {
                    Text(
                        "Knockoff found ${possessive(facts.label)} accounts and categories but couldn't save them: " +
                            discovery.detail,
                    )
                }
            }

        private fun unnamed(facts: DiscoveryFacts): LinkNeeded =
            LinkNeeded(
                title = "Link a device to see ${facts.accounts.list.groupNoun.one} names",
                body =
                    "Knockoff knows some of ${possessive(
                        facts.label,
                    )} ${facts.accounts.list.groupNoun.many} only by ID, because reading their " +
                        "names needs the device link. Set it up in Settings, under Silent holds.",
            )

        private fun needsLink(link: LinkState): Boolean =
            when (link) {
                LinkState.Loading -> {
                    false
                }

                is LinkState.Known -> {
                    when (link.link) {
                        is DeviceLink.Linked -> false
                        DeviceLink.NotLinked -> true
                    }
                }
            }
    }
}
