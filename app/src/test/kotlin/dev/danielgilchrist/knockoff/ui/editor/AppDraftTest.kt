package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.address
import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.group.GroupLabel
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.KnownGroup
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.AccountId
import dev.danielgilchrist.knockoff.domain.rule.AppScope
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.group.GroupDiscovery
import dev.danielgilchrist.knockoff.groupName
import dev.danielgilchrist.knockoff.ruleApps
import dev.danielgilchrist.knockoff.ui.AppDiscovery
import dev.danielgilchrist.knockoff.ui.GrammaticalNumber
import dev.danielgilchrist.knockoff.ui.GroupNoun
import dev.danielgilchrist.knockoff.ui.HeldPhrase
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class AppDraftTest {
    private val whatsapp = COLLEAGUE_CHAT.packageName
    private val gmail = PackageName.GMAIL
    private val work = ChannelGroupId("^ncg_1_account_dan@work.example")
    private val workId = AccountId.Group(work)
    private val twoAccounts =
        mapOf<AccountId, GroupName>(
            workId to groupName("a"),
            AccountId.Group(ChannelGroupId("b")) to groupName("b"),
        )
    private val narrowed = AppDraft.EVERYTHING.copy(mode = AppMode.Narrowed)
    private val boss = ChatPick.of(COLLEAGUE_CHAT.copy(id = ConversationId("2@lid"), name = ConversationName("Boss")))
    private val team = ChatPick.of(COLLEAGUE_CHAT.copy(id = ConversationId("3@lid"), name = ConversationName("Team")))
    private val colleague = ChatPick.of(COLLEAGUE_CHAT)

    @Test
    fun `an app held whole parses to everything and a narrowed one to its picks`() {
        assertEquals(Checked.Ok(AppScope.Everything), AppDraft.EVERYTHING.parse(whatsapp))
        assertEquals(AppDraft.EVERYTHING, AppDraft.from(AppScope.Everything))

        val picked = narrowed.copy(chats = mapOf(colleague.id to colleague), asking = NewChatsAsking.Ask)
        val scope = assertInstanceOf(Checked.Ok::class.java, picked.parse(whatsapp)).value

        assertEquals(picked, AppDraft.from(assertInstanceOf(AppScope.Narrowed::class.java, scope)))
    }

    @Test
    fun `a summary names what an app holds`() {
        assertEquals("All notifications", AppSummary.of(AppDraft.EVERYTHING, none(GroupNoun.Accounts)))
        assertEquals("Nothing chosen", AppSummary.of(narrowed, none(GroupNoun.Categories)))
        assertEquals(
            "dan@work.example",
            AppSummary.of(
                narrowed.copy(accounts = mapOf(workId to groupName("dan@work.example"))),
                none(GroupNoun.Accounts),
            ),
        )
        assertEquals(
            "2 accounts · 1 pattern",
            AppSummary.of(
                narrowed.copy(
                    accounts = twoAccounts,
                    patterns = listOf(PatternDraft.BLANK),
                ),
                none(GroupNoun.Accounts),
            ),
        )
        assertEquals(
            "2 categories",
            AppSummary.of(
                narrowed.copy(
                    accounts = twoAccounts,
                ),
                none(GroupNoun.Categories),
            ),
        )
        assertEquals(
            "Boss, Team · 2 patterns · asks about new ones",
            AppSummary.of(
                narrowed.copy(
                    chats = mapOf(team.id to team, boss.id to boss),
                    patterns = listOf(PatternDraft.BLANK, PatternDraft.BLANK),
                    asking = NewChatsAsking.Ask,
                ),
                none(GroupNoun.Categories),
            ),
        )
        assertEquals(
            "3 chats",
            AppSummary.of(
                narrowed.copy(chats = listOf(boss, team, colleague).associateBy { it.id }),
                none(GroupNoun.Categories),
            ),
        )
        assertEquals(
            "no chats yet · asks about new ones",
            AppSummary.of(narrowed.copy(asking = NewChatsAsking.Ask), none(GroupNoun.Categories)),
        )
        assertEquals(
            "dan@work.example",
            AppSummary.of(
                narrowed.copy(accounts = mapOf(workId to GroupName.of(work))),
                offering(GroupNoun.Accounts, mapOf(workId to groupName("dan@work.example"))),
            ),
        )
    }

    @Test
    fun `an app's kind comes from its known groups and chats or its picks`() {
        val grouped = KnownGroups.NONE.sighted(ChannelKey(gmail, ChannelId("mail")), ChannelGrouping.Grouped(work))
        val seen = SeenChatsState.Loaded(listOf(COLLEAGUE_CHAT))

        assertEquals(AppKind.Plain, kindOf(whatsapp, AppDraft.EVERYTHING))
        assertEquals(AppKind.Accounts, kindOf(gmail, AppDraft.EVERYTHING, groups = grouped))
        assertEquals(AppKind.Chats, kindOf(whatsapp, AppDraft.EVERYTHING, chats = seen))
        assertEquals(AppKind.Plain, kindOf(gmail, AppDraft.EVERYTHING, chats = seen))
        assertEquals(
            AppKind.AccountsAndChats,
            kindOf(
                whatsapp,
                narrowed.copy(
                    accounts = mapOf(workId to groupName("a")),
                    chats =
                        mapOf(boss.id to boss),
                ),
            ),
        )
        assertEquals(listOf(false, true, false, true), AppKind.entries.map { it.hasAccounts })
        assertEquals(listOf(false, false, true, true), AppKind.entries.map { it.hasChats })
    }

    @Test
    fun `an app shows chats when reading them failed, but not while they load`() {
        assertEquals(AppKind.Plain, kindOf(whatsapp, AppDraft.EVERYTHING, chats = SeenChatsState.Loading))
        assertEquals(AppKind.Chats, kindOf(whatsapp, AppDraft.EVERYTHING, chats = SeenChatsState.Failed("x")))
        assertEquals(AppKind.Chats, kindOf(whatsapp, narrowed.copy(asking = NewChatsAsking.Ask)))
    }

    @Test
    fun `accounts the stored rule picks keep an app's accounts and noun after they're unticked`() {
        val stored =
            ruleApps(setOf(Target.Account(gmail, work, groupName("dan@work.example"))), emptySet())
        val accounts = AccountChoices.forApp(gmail, KnownGroups.NONE, stored, narrowed)

        assertEquals(AppKind.Accounts, AppKind.of(gmail, narrowed, accounts, SeenChatsState.Loaded(emptyList())))
        assertEquals(GroupNoun.Accounts, accounts.noun)
        assertEquals(mapOf(workId to groupName("dan@work.example")), accounts.offered)
    }

    @Test
    fun `a picked account the phone hasn't shown is flagged as unseen, or missing once the app is read`() {
        val home = ChannelGroupId("^ncg_1_account_dan@home.example")
        val stored =
            ruleApps(setOf(Target.Account(gmail, work, groupName("dan@work.example"))), emptySet())
        val homeKnown = KnownGroups.NONE.sighted(ChannelKey(gmail, ChannelId("home")), ChannelGrouping.Grouped(home))
        val bothKnown = homeKnown.sighted(ChannelKey(gmail, ChannelId("work")), ChannelGrouping.Grouped(work))
        val unseen = AccountChoices.forApp(gmail, homeKnown, stored, narrowed)
        val seen = AccountChoices.forApp(gmail, bothKnown, stored, narrowed)

        val read = AppDiscovery.Finished(GroupDiscovery.Read)

        assertEquals(AccountSighting.NotChecked, unseen.sightingOf(workId, AppDiscovery.NotAsked))
        assertEquals(AccountSighting.NotChecked, unseen.sightingOf(workId, AppDiscovery.Reading))
        assertEquals(
            List(4) { AccountSighting.CouldNotCheck },
            listOf(
                GroupDiscovery.NotLinked("no association"),
                GroupDiscovery.Unbound,
                GroupDiscovery.Unrecorded("disk"),
                GroupDiscovery.Unavailable("binder full"),
            ).map { unseen.sightingOf(workId, AppDiscovery.Finished(it)) },
        )
        assertEquals(AccountSighting.NotOnPhone, unseen.sightingOf(workId, read))
        assertEquals(AccountSighting.Seen, unseen.sightingOf(AccountId.Group(home), read))
        assertEquals(AccountSighting.Seen, seen.sightingOf(workId, read))
    }

    @Test
    fun `an app with nothing picked says what to tick and is flagged in the list`() {
        assertEquals("Add at least one pattern.", unchosenCaption(AppKind.Plain, GroupNoun.Categories))
        assertEquals("Tick at least one account.", unchosenCaption(AppKind.Accounts, GroupNoun.Accounts))
        assertEquals(
            "Tick at least one chat, or turn on Ask me about new chats.",
            unchosenCaption(AppKind.Chats, GroupNoun.Categories),
        )
        assertEquals(
            "Tick at least one category or chat, or turn on Ask me about new chats.",
            unchosenCaption(AppKind.AccountsAndChats, GroupNoun.Categories),
        )
        assertEquals(
            listOf("No patterns added", "No accounts picked", "No chats picked", "No accounts or chats picked"),
            AppKind.entries.map { AppSummary.unpicked(it, GroupNoun.Accounts) },
        )
        assertEquals("No categories picked", AppSummary.unpicked(AppKind.Accounts, GroupNoun.Categories))
    }

    @Test
    fun `narrowing is offered with anything to choose or picks to keep, so a pattern-only app survives All and back`() {
        val patterned = narrowed.copy(patterns = listOf(PatternDraft(PatternField.ChannelId, "^mail")))
        val whole = patterned.copy(mode = AppMode.Everything)

        assertEquals(true, patterned.offersNarrowing(0))
        assertEquals(true, whole.offersNarrowing(0))
        assertEquals(patterned, whole.copy(mode = AppMode.Narrowed))
        assertEquals(Checked.Ok(AppScope.Everything), whole.parse(gmail))
        assertEquals(true, narrowed.offersNarrowing(0))
        assertEquals(false, AppDraft.EVERYTHING.offersNarrowing(0))
        assertEquals(true, AppDraft.EVERYTHING.offersNarrowing(1))
        assertEquals(
            listOf(false, true, true, true, true),
            listOf(
                AppDraft.EVERYTHING,
                AppDraft.EVERYTHING.copy(accounts = mapOf(workId to groupName("a"))),
                AppDraft.EVERYTHING.copy(chats = mapOf(boss.id to boss)),
                AppDraft.EVERYTHING.copy(patterns = listOf(PatternDraft.BLANK)),
                AppDraft.EVERYTHING.copy(asking = NewChatsAsking.Ask),
            ).map { it.picksSomething },
        )
    }

    @Test
    fun `without a choice to make, the caption says more will show once they notify`() {
        assertEquals(
            "If Gmail has more accounts, they'll show here once each has notified you.",
            moreCaption("Gmail", AppKind.Accounts, GroupNoun.Accounts),
        )
        assertEquals(
            "If Slack has more categories and chats, they'll show here once each has notified you.",
            moreCaption("Slack", AppKind.AccountsAndChats, GroupNoun.Categories),
        )
        assertEquals(
            "If WhatsApp has more chats, they'll show here once each has notified you.",
            moreCaption("WhatsApp", AppKind.Chats, GroupNoun.Categories),
        )
        assertEquals(
            "Knockoff learns an app's accounts and chats from its notifications. If Maps has them, they'll show " +
                "here once it has notified you.",
            moreCaption("Maps", AppKind.Plain, GroupNoun.Categories),
        )
        assertEquals("notifications", AppKind.Plain.more(GroupNoun.Categories))
    }

    @Test
    fun `the narrowed choice and what to pick are worded by capability and noun`() {
        assertEquals(
            listOf(
                "Only ones matching a pattern",
                "Only some accounts",
                "Only some chats",
                "Only some accounts and chats",
            ),
            AppKind.entries.map { it.narrowed(GroupNoun.Accounts) },
        )
        assertEquals("Only some categories", AppKind.Accounts.narrowed(GroupNoun.Categories))
        assertEquals(
            listOf("pattern", "category", "chat", "category or chat"),
            AppKind.entries.map { it.pick(GroupNoun.Categories) },
        )
        assertEquals("account", AppKind.Accounts.pick(GroupNoun.Accounts))
    }

    @Test
    fun `the narrowed choice says how many there are and that the rule leaves the rest alone`() {
        val accounts =
            offering(GroupNoun.Accounts, mapOf(workId to groupName("dan@work.example")))
        val none = offering(GroupNoun.Categories, emptyMap())

        assertEquals(
            "Choose from 1 account. This rule leaves the rest alone.",
            narrowedSupporting(AppKind.Accounts, accounts, false),
        )
        assertEquals("This rule leaves the rest alone.", narrowedSupporting(AppKind.Chats, accounts, false))
        assertEquals("This rule leaves the rest alone.", narrowedSupporting(AppKind.Accounts, none, false))
        assertEquals(
            "This rule leaves the rest alone.",
            narrowedSupporting(AppKind.AccountsAndChats, accounts, true),
        )
        assertEquals("This rule leaves the rest alone.", narrowedSupporting(AppKind.Plain, none, false))
    }

    @Test
    fun `an app's caption names the app when whole and what's picked when narrowed`() {
        assertEquals(
            HeldPhrase.of("WhatsApp", GrammaticalNumber.One),
            appHeld("WhatsApp", AppMode.Everything, AppKind.Chats, GroupNoun.Categories),
        )
        assertEquals(
            HeldPhrase.of("The chats you pick", GrammaticalNumber.Many),
            appHeld("WhatsApp", AppMode.Narrowed, AppKind.Chats, GroupNoun.Categories),
        )
        assertEquals(
            HeldPhrase.of("The accounts you pick", GrammaticalNumber.Many),
            appHeld("Gmail", AppMode.Narrowed, AppKind.Accounts, GroupNoun.Accounts),
        )
        assertEquals(
            HeldPhrase.of("Notifications matching your patterns", GrammaticalNumber.Many),
            appHeld("Slack", AppMode.Narrowed, AppKind.Plain, GroupNoun.Categories),
        )
    }

    @Test
    fun `accounts list known groups by name, or id when unnamed, plus picked and stored ones`() {
        val named = KnownGroup(ChannelGroupId("chats"), GroupLabel.Named(groupName("Chats")))
        val unnamed = KnownGroup(ChannelGroupId("misc"), GroupLabel.Unnamed)
        val stored = mapOf<AccountId, GroupName>(AccountId.Group(ChannelGroupId("gone")) to groupName("Gone"))
        val picked = narrowed.copy(accounts = mapOf(AccountId.Group(ChannelGroupId("old")) to groupName("Old")))

        assertEquals(
            AccountChoices(
                AccountList.Single(
                    GroupNoun.Categories,
                    mapOf(
                        AccountId.Group(ChannelGroupId("chats")) to groupName("Chats"),
                        AccountId.Group(ChannelGroupId("misc")) to groupName("misc"),
                        AccountId.Group(ChannelGroupId("gone")) to groupName("Gone"),
                        AccountId.Group(ChannelGroupId("old")) to groupName("Old"),
                    ),
                ),
                setOf(AccountId.Group(ChannelGroupId("gone")), AccountId.Group(ChannelGroupId("old"))),
                GroupNaming.SomeUnnamed,
            ),
            AccountChoices.of(listOf(named, unnamed), emptyList(), stored, picked),
        )
    }

    @Test
    fun `an app with addresses offers only accounts, keeps picked groups and prefers learned names`() {
        val account = KnownGroup(work, GroupLabel.Named(groupName("dan@work.example")))
        val misc = KnownGroup(ChannelGroupId("^ncg_~_misc"), GroupLabel.Named(groupName("Miscellaneous")))
        val other = KnownGroup(ChannelGroupId("other"), GroupLabel.Unnamed)
        val picked =
            narrowed.copy(
                accounts =
                    mapOf(
                        workId to GroupName.of(work),
                        AccountId.Group(ChannelGroupId("other")) to groupName("Other"),
                    ),
            )

        assertEquals(
            AccountChoices(
                AccountList.Single(
                    GroupNoun.Accounts,
                    mapOf(
                        workId to groupName("dan@work.example"),
                        AccountId.Group(ChannelGroupId("other")) to groupName("Other"),
                    ),
                ),
                emptySet(),
                GroupNaming.SomeUnnamed,
            ),
            AccountChoices.of(listOf(account, misc, other), emptyList(), emptyMap(), picked),
        )
        assertEquals(
            mapOf(
                workId to groupName("dan@work.example"),
                AccountId.Group(ChannelGroupId("other")) to groupName("Other"),
            ),
            picked.withLearnedNames(AccountChoices.learnedNames(listOf(account, misc, other))).accounts,
        )
    }

    @Test
    fun `addresses seen in notifications are offered as accounts unless a group already names them`() {
        val proton = PackageName("ch.protonmail.android")
        val work = address("dan@work.example")
        val home = address("dan@home.example")
        val gmailWork = KnownGroup(this.work, GroupLabel.Named(groupName("Dan@Work.Example")))
        val choices = AccountChoices.of(listOf(gmailWork), listOf(work, home), emptyMap(), narrowed)

        assertEquals(GroupNoun.Accounts, choices.noun)
        assertEquals(
            mapOf(
                workId to groupName("Dan@Work.Example"),
                AccountId.Address(home) to GroupName.of(home),
            ),
            choices.offered,
        )
        assertEquals(
            mapOf(AccountId.Address(work) to GroupName.of(work)),
            AccountChoices.of(emptyList(), listOf(work), emptyMap(), narrowed).offered,
        )

        val picked = narrowed.copy(accounts = mapOf(AccountId.Address(home) to GroupName.of(home)))
        val scope = assertInstanceOf(Checked.Ok::class.java, picked.parse(proton)).value

        assertEquals(
            setOf(Target.SubTextAccount(proton, home)),
            RuleApps.of(mapOf(proton to assertInstanceOf(AppScope.Narrowed::class.java, scope))).targets,
        )
    }

    @Test
    fun `a kept address not seen since reads as not in a notification yet, whatever discovery found`() {
        val gone = address("old@work.example")
        val choices =
            AccountChoices.of(
                emptyList(),
                listOf(address("dan@work.example")),
                mapOf(AccountId.Address(gone) to GroupName.of(gone)),
                narrowed,
            )

        assertEquals(
            listOf(AccountSighting.NotNotified, AccountSighting.NotNotified),
            listOf(AppDiscovery.NotAsked, AppDiscovery.Finished(GroupDiscovery.Read)).map {
                choices.sightingOf(AccountId.Address(gone), it)
            },
        )
        assertEquals(
            AccountSighting.Seen,
            choices.sightingOf(AccountId.Address(address("dan@work.example")), AppDiscovery.NotAsked),
        )
        assertEquals(
            mapOf(AccountId.Address(gone) to GroupName.of(gone)),
            narrowed
                .copy(accounts = mapOf(AccountId.Address(gone) to GroupName.of(gone)))
                .withLearnedNames(mapOf(ChannelGroupId(gone.value) to groupName("Renamed")))
                .accounts,
        )
    }

    @Test
    fun `nouns read accounts only when a name is an address`() {
        assertEquals(GroupNoun.Accounts, GroupNoun.of(listOf(groupName("Chats"), groupName("dan@work.example"))))
        assertEquals(GroupNoun.Categories, GroupNoun.of(listOf(groupName("Chats"))))
        assertEquals("1 category", GroupNoun.Categories.counted(1))
        assertEquals("2 accounts", GroupNoun.Accounts.counted(2))
    }

    private fun offering(
        noun: GroupNoun,
        offered: Map<AccountId, GroupName>,
    ): AccountChoices = AccountChoices(AccountList.Single(noun, offered), emptySet(), GroupNaming.AllNamed)

    private fun none(noun: GroupNoun): AccountChoices = offering(noun, emptyMap())

    private fun kindOf(
        app: PackageName,
        draft: AppDraft,
        groups: KnownGroups = KnownGroups.NONE,
        chats: SeenChatsState = SeenChatsState.Loaded(emptyList()),
    ): AppKind = AppKind.of(app, draft, AccountChoices.forApp(app, groups, RuleApps.NONE, draft), chats)
}
