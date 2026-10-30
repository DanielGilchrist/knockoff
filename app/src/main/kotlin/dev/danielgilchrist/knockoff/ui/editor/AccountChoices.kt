package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.group.GroupLabel
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.KnownGroup
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.AccountId
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.group.GroupDiscovery
import dev.danielgilchrist.knockoff.ui.AppDiscovery
import dev.danielgilchrist.knockoff.ui.GroupNoun

data class AccountChoices(
    val list: AccountList,
    val unseen: Set<AccountId>,
    val naming: GroupNaming,
) {
    val noun: GroupNoun get() = list.noun

    val offered: Map<AccountId, GroupName> get() = list.offered

    companion object {
        fun of(
            known: List<KnownGroup>,
            addresses: List<AccountAddress>,
            stored: Map<AccountId, GroupName>,
            draft: AppDraft,
        ): AccountChoices {
            val learned = learnedNames(known)
            val knownNames = known.associate { AccountId.Group(it.id) to nameOf(it) }
            val kept = (stored + draft.accounts).mapValues { (id, name) -> renamed(id, name, learned) }
            val keptGroups = kept.filterKeys { it is AccountId.Group }
            val groupNames = knownNames.values + keptGroups.values
            val groupNoun = GroupNoun.of(groupNames)
            val keptAddresses =
                kept.keys.flatMap { id ->
                    when (id) {
                        is AccountId.Group -> emptyList()
                        is AccountId.Address -> listOf(id.address)
                    }
                }
            val shown: Map<AccountId, GroupName> =
                (addresses + keptAddresses)
                    .distinct()
                    .filter { address ->
                        AccountId.Address(address) in draft.accounts || groupNames.none { it.names(address) }
                    }.associate { address ->
                        AccountId.Address(address) to
                            kept.getOrDefault(AccountId.Address(address), GroupName.of(address))
                    }
            val seen = knownNames.keys + addresses.map(AccountId::Address)

            val naming =
                when {
                    known.any { it.label == GroupLabel.Unnamed } -> GroupNaming.SomeUnnamed
                    else -> GroupNaming.AllNamed
                }

            return AccountChoices(
                AccountList.of(groupNoun, knownNames.filterValues(groupNoun::names) + keptGroups, shown),
                kept.keys - seen,
                naming,
            )
        }

        fun forApp(
            app: PackageName,
            groups: KnownGroups,
            stored: RuleApps,
            draft: AppDraft,
        ): AccountChoices = of(groups.groupsOf(app), groups.addressesOf(app), stored.accountsOf(app), draft)

        fun renamed(
            id: AccountId,
            name: GroupName,
            learned: Map<ChannelGroupId, GroupName>,
        ): GroupName =
            when (id) {
                is AccountId.Group -> learned.getOrDefault(id.id, name)
                is AccountId.Address -> name
            }

        fun learnedNames(known: List<KnownGroup>): Map<ChannelGroupId, GroupName> =
            known
                .flatMap { group ->
                    when (val label = group.label) {
                        is GroupLabel.Named -> listOf(group.id to label.name)
                        GroupLabel.Unnamed -> emptyList()
                    }
                }.toMap()

        private fun nameOf(group: KnownGroup): GroupName =
            when (val label = group.label) {
                is GroupLabel.Named -> label.name
                GroupLabel.Unnamed -> GroupName.of(group.id)
            }
    }

    fun sightingOf(
        id: AccountId,
        discovery: AppDiscovery,
    ): AccountSighting =
        when {
            id !in unseen -> AccountSighting.Seen
            else -> unseenSighting(id, discovery)
        }

    private fun unseenSighting(
        id: AccountId,
        discovery: AppDiscovery,
    ): AccountSighting =
        when (id) {
            is AccountId.Group -> unseenSighting(discovery)
            is AccountId.Address -> AccountSighting.NotNotified
        }

    private fun unseenSighting(discovery: AppDiscovery): AccountSighting =
        when (discovery) {
            AppDiscovery.NotAsked, AppDiscovery.Reading -> {
                AccountSighting.NotChecked
            }

            is AppDiscovery.Finished -> {
                when (discovery.discovery) {
                    GroupDiscovery.Read -> AccountSighting.NotOnPhone

                    is GroupDiscovery.NotLinked,
                    GroupDiscovery.Unbound,
                    is GroupDiscovery.Unavailable,
                    is GroupDiscovery.Unrecorded,
                    -> AccountSighting.CouldNotCheck
                }
            }
        }
}
