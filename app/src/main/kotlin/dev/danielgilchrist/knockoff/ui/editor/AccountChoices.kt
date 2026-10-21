package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.group.GroupLabel
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.KnownGroup
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.ui.GroupNoun

data class AccountChoices(
    val noun: GroupNoun,
    val offered: Map<ChannelGroupId, GroupName>,
) {
    companion object {
        fun of(
            known: List<KnownGroup>,
            stored: Map<ChannelGroupId, GroupName>,
            draft: AppDraft,
        ): AccountChoices {
            val learned = learnedNames(known)
            val kept = (stored + draft.accounts).mapValues { (id, name) -> learned.getOrDefault(id, name) }
            val noun = GroupNoun.of(known.map(::nameOf) + kept.values)
            val listed = known.associate { it.id to nameOf(it) }.filterValues(noun::names)

            return AccountChoices(noun, listed + kept)
        }

        fun forApp(
            app: PackageName,
            groups: KnownGroups,
            stored: RuleApps,
            draft: AppDraft,
        ): AccountChoices = of(groups.groupsOf(app), stored.accountsOf(app), draft)

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
}
