package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.EVERY_DAY
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.address
import dev.danielgilchrist.knockoff.domain.breakNamed
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.days
import dev.danielgilchrist.knockoff.domain.gmailAccount
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.GroupNameParse
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.rule.AccountId
import dev.danielgilchrist.knockoff.domain.rule.AppScope
import dev.danielgilchrist.knockoff.domain.rule.NarrowedParse
import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import dev.danielgilchrist.knockoff.domain.textPattern
import dev.danielgilchrist.knockoff.domain.window
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.DayOfWeek
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.ZoneId

class SettingsTest {
    companion object {
        private const val MONDAY = """{"days": ["MONDAY"], "start": "08:00", "end": "17:00"}"""
        private const val WORK_PATTERN = """{"app": "com.whatsapp", "field": "chatName", "pattern": "^Work"}"""

        @JvmStatic
        fun invalidRules(): List<Arguments> =
            listOf(
                Arguments.of("""{"name": " "}""", "rule 1: blank name"),
                Arguments.of("""{"state": "Paused"}""", "rule 1: \"Work\": unknown state \"Paused\""),
                Arguments.of("""{"policy": "Mute"}""", "rule 1: \"Work\": unknown policy \"Mute\""),
                Arguments.of("""{"windows": []}""", "rule 1: \"Work\": no windows"),
                Arguments.of(
                    """{"windows": [$MONDAY, {"days": ["FUNDAY"], "start": "08:00", "end": "17:00"}]}""",
                    "rule 1: \"Work\": window 2: unknown days [FUNDAY]",
                ),
                Arguments.of(
                    """{"windows": [{"days": [], "start": "08:00", "end": "17:00"}]}""",
                    "rule 1: \"Work\": window 1: no days",
                ),
                Arguments.of(
                    """{"windows": [{"days": ["MONDAY"], "start": "8am", "end": "17:00"}]}""",
                    "rule 1: \"Work\": window 1: \"8am\" is not a time of day",
                ),
                Arguments.of(
                    """{"windows": [{"days": ["MONDAY"], "start": "08:00", "end": "25:00"}]}""",
                    "rule 1: \"Work\": window 1: \"25:00\" is not a time of day",
                ),
            )
    }

    @Test
    fun `default is a work rule for weekday office hours with nothing targeted yet`() {
        Settings.DEFAULT.rules shouldBe ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, emptySet()))
    }

    @Test
    fun `every kind of rule round-trips`() {
        val settings =
            Settings(
                ruleSet(
                    rule(
                        "Work",
                        Policy.DeliverOnlyDuring,
                        WORK_HOURS,
                        setOf(
                            Target.App(SLACK),
                            WORK_ADDRESS.gmailAccount(),
                            COLLEAGUE_CHAT,
                            Target.ChannelPattern(PackageName.GMAIL, textPattern("_dan@work\\.example$")),
                            Target.ChatNamePattern(WHATSAPP, textPattern("^Work")),
                        ),
                    ),
                    rule(
                        "Nights",
                        Policy.HoldDuring,
                        schedule(window(EVERY_DAY, "22:00", "06:30"), window(days(SATURDAY, SUNDAY), "09:00", "09:00")),
                        setOf(Target.App(PackageName.GMAIL)),
                        RuleState.Disabled,
                    ),
                    rule(
                        "Lunch",
                        Policy.DeliverOnlyDuring,
                        schedule(
                            window(EVERY_DAY, "08:00", "17:00"),
                            breaks =
                                listOf(
                                    breakNamed("Lunch", window(EVERY_DAY, "12:00", "13:00")),
                                    breakNamed("Focus", window(days(SATURDAY), "14:00", "16:00")),
                                ),
                        ),
                        emptySet(),
                    ),
                    rule(
                        "Precise",
                        Policy.HoldDuring,
                        schedule(window(EVERY_DAY, "12:00:30", "12:00:00.5")),
                        emptySet(),
                        zone = RuleZone.Chosen(ZoneChoice.Fixed(ZoneId.of("Asia/Tokyo"))),
                    ),
                    rule(
                        "Local",
                        Policy.HoldDuring,
                        WORK_HOURS,
                        emptySet(),
                        zone = RuleZone.Chosen(ZoneChoice.Device),
                        asking = setOf(WHATSAPP, SLACK),
                    ),
                ).withDefaultZone(ZoneChoice.Fixed(ZoneId.of("Europe/London"))),
                Theme.Dark,
                HolidayCalendar.None,
            )

        Settings.fromJson(settings.toJson()) shouldBe SettingsDecode.Decoded(settings)
    }

    @Test
    fun `no rules round-trips`() {
        val empty = Settings(ruleSet(), Theme.Light, HolidayCalendar.None)

        Settings.fromJson(empty.toJson()) shouldBe SettingsDecode.Decoded(empty)
    }

    @Test
    fun `the stored document is stable`() {
        Settings.DEFAULT.toJson() shouldBe
            """
            {
                "version": 9,
                "defaultZone": "device",
                "theme": "System",
                "holidayCalendar": {
                    "type": "none"
                },
                "rules": [
                    {
                        "name": "Work",
                        "state": "Enabled",
                        "policy": "DeliverOnlyDuring",
                        "windows": [
                            {
                                "days": [
                                    "MONDAY",
                                    "TUESDAY",
                                    "WEDNESDAY",
                                    "THURSDAY",
                                    "FRIDAY"
                                ],
                                "start": "08:00",
                                "end": "17:00"
                            }
                        ],
                        "breaks": [],
                        "zone": "default",
                        "apps": [],
                        "daysOff": [],
                        "publicHolidays": "AsUsual"
                    }
                ]
            }
            """.trimIndent()
    }

    @Test
    fun `version 1 documents migrate to the device zone and system theme`() {
        val rule = ruleJson("""{"apps": ["com.Slack"], "gmailAccounts": ["dan@work.example"]}""")

        Settings.fromJson("""{"version": 1, "rules": [$rule]}""") shouldBe
            SettingsDecode.Decoded(
                Settings(
                    ruleSet(
                        rule(
                            "Work",
                            Policy.DeliverOnlyDuring,
                            schedule(window(days(DayOfWeek.MONDAY), "08:00", "17:00")),
                            setOf(Target.App(SLACK), WORK_ADDRESS.gmailAccount()),
                        ),
                    ),
                    Theme.System,
                    HolidayCalendar.None,
                ),
            )
    }

    @Test
    fun `version 6 rules are regrouped by app`() {
        val chat =
            """{"app": "com.whatsapp", "channel": "individual_chat_defaults_4", "id": "1000000000001@lid",
            "name": "Colleague"}"""
        val slackChat = chat.replace("com.whatsapp", "com.Slack")
        val rule =
            ruleJson(
                """{"apps": ["com.Slack"], "gmailAccounts": ["dan@work.example"], "chats": [$chat, $slackChat],
                "patterns": [$WORK_PATTERN], "breaks": [], "zone": "default",
                "asking": ["com.whatsapp", "com.Slack", "org.signal"]}""",
            )

        Settings.fromJson("""{"version": 6, "defaultZone": "device", "theme": "System", "rules": [$rule]}""") shouldBe
            SettingsDecode.Decoded(
                Settings(
                    ruleSet(
                        rule(
                            "Work",
                            Policy.DeliverOnlyDuring,
                            schedule(window(days(DayOfWeek.MONDAY), "08:00", "17:00")),
                            setOf(
                                Target.App(SLACK),
                                WORK_ADDRESS.gmailAccount(),
                                COLLEAGUE_CHAT,
                                Target.ChatNamePattern(WHATSAPP, textPattern("^Work")),
                            ),
                            asking = setOf(WHATSAPP, PackageName("org.signal")),
                        ),
                    ),
                    Theme.System,
                    HolidayCalendar.None,
                ),
            )
    }

    @Test
    fun `a version 6 Gmail address becomes its account group whatever its shape`() {
        val rule =
            ruleJson(
                """{"gmailAccounts": ["dan"], "breaks": [], "chats": [], "patterns": [], "zone": "default",
                "asking": []}""",
            )

        Settings
            .fromJson("""{"version": 6, "defaultZone": "device", "theme": "System", "rules": [$rule]}""")
            .shouldBeInstanceOf<SettingsDecode.Decoded>()
            .settings.rules.rules
            .single()
            .apps.scopes shouldBe
            mapOf(
                PackageName.GMAIL to
                    AppScope.Narrowed
                        .parse(
                            mapOf(
                                AccountId.Group(ChannelGroupId("^ncg_1_account_dan")) to
                                    GroupName.parse("dan").shouldBeInstanceOf<GroupNameParse.Valid>().name,
                            ),
                            emptyList(),
                            emptySet(),
                            NewChatsAsking.DontAsk,
                        ).shouldBeInstanceOf<NarrowedParse.Valid>()
                        .scope,
            )
    }

    @Test
    fun `version 5 documents migrate asking about nothing`() {
        val rule =
            ruleJson(
                """{"apps": ["com.Slack"], "breaks": [], "chats": [], "patterns": [$WORK_PATTERN], "zone": "device"}""",
            )

        Settings.fromJson("""{"version": 5, "defaultZone": "device", "theme": "Dark", "rules": [$rule]}""") shouldBe
            SettingsDecode.Decoded(
                Settings(
                    ruleSet(
                        rule(
                            "Work",
                            Policy.DeliverOnlyDuring,
                            schedule(window(days(DayOfWeek.MONDAY), "08:00", "17:00")),
                            setOf(Target.App(SLACK), Target.ChatNamePattern(WHATSAPP, textPattern("^Work"))),
                            zone = RuleZone.Chosen(ZoneChoice.Device),
                        ),
                    ),
                    Theme.Dark,
                    HolidayCalendar.None,
                ),
            )
    }

    @Test
    fun `version 4 documents migrate with no patterns`() {
        val rule = ruleJson("""{"apps": ["com.Slack"], "breaks": [], "chats": [], "zone": "device"}""")

        Settings.fromJson("""{"version": 4, "defaultZone": "device", "theme": "Dark", "rules": [$rule]}""") shouldBe
            SettingsDecode.Decoded(
                Settings(
                    ruleSet(
                        rule(
                            "Work",
                            Policy.DeliverOnlyDuring,
                            schedule(window(days(DayOfWeek.MONDAY), "08:00", "17:00")),
                            setOf(Target.App(SLACK)),
                            zone = RuleZone.Chosen(ZoneChoice.Device),
                        ),
                    ),
                    Theme.Dark,
                    HolidayCalendar.None,
                ),
            )
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource(
        "chatName, ' ', blank pattern",
        "chatName, (Work, invalid pattern \"(Work\": Unclosed group",
        "title, Work, unknown pattern field \"title\"",
    )
    fun `invalid patterns explain why`(
        field: String,
        source: String,
        problem: String,
    ) {
        val pattern = """{"field": "$field", "pattern": "$source"}"""

        Settings
            .fromJson(
                appsDocument(narrowedApp("com.whatsapp", patterns = pattern)),
            ).shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe
            "rule 1: \"Work\": app 1: \"com.whatsapp\": pattern 1: $problem"
    }

    @Test
    fun `version 3 documents migrate with no breaks`() {
        val rule = ruleJson("""{"apps": ["com.Slack"], "chats": [], "zone": "device"}""")

        Settings.fromJson("""{"version": 3, "defaultZone": "device", "theme": "Dark", "rules": [$rule]}""") shouldBe
            SettingsDecode.Decoded(
                Settings(
                    ruleSet(
                        rule(
                            "Work",
                            Policy.DeliverOnlyDuring,
                            schedule(window(days(DayOfWeek.MONDAY), "08:00", "17:00")),
                            setOf(Target.App(SLACK)),
                            zone = RuleZone.Chosen(ZoneChoice.Device),
                        ),
                    ),
                    Theme.Dark,
                    HolidayCalendar.None,
                ),
            )
    }

    @Test
    fun `a break without a name or covering all the hours is invalid`() {
        val blank = """{"name": " ", "window": $MONDAY}"""
        val allDay = """{"name": "Off", "window": $MONDAY}"""

        Settings.fromJson(currentDocument(breaks = blank)).shouldBeInstanceOf<SettingsDecode.Invalid>().detail shouldBe
            "rule 1: \"Work\": break 1: blank break name"
        Settings.fromJson(currentDocument(breaks = allDay)).shouldBeInstanceOf<SettingsDecode.Invalid>().detail shouldBe
            "rule 1: \"Work\": breaks cover all of its hours"
    }

    @Test
    fun `version 2 documents migrate with no chats`() {
        val rule = ruleJson("""{"apps": ["com.Slack"], "gmailAccounts": ["dan@work.example"], "zone": "device"}""")

        Settings.fromJson("""{"version": 2, "defaultZone": "device", "theme": "Dark", "rules": [$rule]}""") shouldBe
            SettingsDecode.Decoded(
                Settings(
                    ruleSet(
                        rule(
                            "Work",
                            Policy.DeliverOnlyDuring,
                            schedule(window(days(DayOfWeek.MONDAY), "08:00", "17:00")),
                            setOf(Target.App(SLACK), WORK_ADDRESS.gmailAccount()),
                            zone = RuleZone.Chosen(ZoneChoice.Device),
                        ),
                    ),
                    Theme.Dark,
                    HolidayCalendar.None,
                ),
            )
    }

    @ParameterizedTest(name = "{3}")
    @CsvSource(
        "' ', 1000000000001@lid, Someone, blank chat channel",
        "chats, ' ', Someone, blank chat id",
        "chats, 1000000000001@lid, ' ', blank chat name",
    )
    fun `a chat missing a part is invalid`(
        channel: String,
        id: String,
        name: String,
        problem: String,
    ) {
        val chat = """{"channel": "$channel", "id": "$id", "name": "$name"}"""

        Settings
            .fromJson(appsDocument(narrowedApp("com.whatsapp", chats = chat)))
            .shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe
            "rule 1: \"Work\": app 1: \"com.whatsapp\": chat 1: $problem"
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource(
        "' ', a, blank account group",
        "^ncg_1_account_a, ' ', blank account label",
    )
    fun `an account missing a part is invalid`(
        group: String,
        label: String,
        problem: String,
    ) {
        val account = """{"group": "$group", "label": "$label"}"""

        Settings
            .fromJson(appsDocument(narrowedApp("com.google.android.gm", accounts = account)))
            .shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe
            "rule 1: \"Work\": app 1: \"com.google.android.gm\": account 1: $problem"
    }

    @Test
    fun `an app's scope must be valid and each app listed once`() {
        val everything = """{"app": "com.Slack", "scope": {"type": "everything"}}"""
        val account = """{"group": "^ncg_1_account_a@b.c", "label": "a@b.c"}"""

        Settings
            .fromJson(appsDocument("""{"app": " ", "scope": {"type": "everything"}}"""))
            .shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe "rule 1: \"Work\": app 1: blank app"
        Settings
            .fromJson(appsDocument("$everything, $everything"))
            .shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe "rule 1: \"Work\": duplicate apps [com.Slack]"
        Settings
            .fromJson(appsDocument(narrowedApp("com.whatsapp")))
            .shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe "rule 1: \"Work\": app 1: \"com.whatsapp\": narrowed to nothing"
        Settings
            .fromJson(appsDocument(narrowedApp("com.google.android.gm", accounts = "$account, $account")))
            .shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe
            "rule 1: \"Work\": app 1: \"com.google.android.gm\": duplicate accounts [^ncg_1_account_a@b.c]"
    }

    @Test
    fun `version 8 apps can be narrowed to addresses, which round-trip`() {
        val decode =
            Settings
                .fromJson(appsDocument(addressedApp("ch.protonmail.android", "\" Dan@Work.Example \""), version = 8))
                .shouldBeInstanceOf<SettingsDecode.Decoded>()
        val work = address("dan@work.example")

        decode.settings.rules.rules
            .single()
            .apps
            .accountsOf(PackageName("ch.protonmail.android")) shouldBe
            mapOf(AccountId.Address(work) to GroupName.of(work))
        Settings.fromJson(decode.settings.toJson()) shouldBe decode
    }

    @Test
    fun `an address that isn't one, or listed twice, is invalid`() {
        Settings
            .fromJson(appsDocument(addressedApp("ch.protonmail.android", "\"work\""), version = 8))
            .shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe "rule 1: \"Work\": app 1: \"ch.protonmail.android\": address 1: \"work\" isn't an address"
        Settings
            .fromJson(
                appsDocument(
                    addressedApp("ch.protonmail.android", "\"dan@work.example\", \"DAN@work.example\""),
                    version = 8,
                ),
            ).shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe "rule 1: \"Work\": app 1: \"ch.protonmail.android\": duplicate accounts [dan@work.example]"
    }

    @Test
    fun `version 7 documents have no addresses to read`() {
        Settings
            .fromJson(appsDocument(addressedApp("ch.protonmail.android", "\"dan@work.example\"")))
            .shouldBeInstanceOf<SettingsDecode.Unreadable>()
    }

    @Test
    fun `the same chat twice is invalid whatever its name`() {
        val chat = """{"channel": "chats", "id": "1000000000001@lid", "name": "Someone"}"""
        val renamed = chat.replace("Someone", "Someone else")

        Settings
            .fromJson(
                appsDocument(narrowedApp("com.whatsapp", chats = "$chat, $renamed")),
            ).shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe
            "rule 1: \"Work\": app 1: \"com.whatsapp\": duplicate chats [1000000000001@lid]"
    }

    @Test
    fun `unknown zones and themes are invalid`() {
        val zoned = ruleJson("""{"breaks": [], "chats": [], "patterns": [], "zone": "Mars/Base", "asking": []}""")
        val unknown = "java.time.zone.ZoneRulesException: Unknown time-zone ID: Mars/Base"

        Settings.fromJson("""{"version": 6, "defaultZone": "Mars/Base", "theme": "System", "rules": []}""") shouldBe
            SettingsDecode.Invalid("unknown default zone \"Mars/Base\": $unknown")
        Settings.fromJson("""{"version": 6, "defaultZone": "device", "theme": "Neon", "rules": []}""") shouldBe
            SettingsDecode.Invalid("unknown theme \"Neon\"")
        Settings
            .fromJson("""{"version": 6, "defaultZone": "device", "theme": "System", "rules": [$zoned]}""")
            .shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe "rule 1: \"Work\": unknown zone \"Mars/Base\": $unknown"
    }

    @Test
    fun `a version 2 rule must say its zone and a version 1 rule can't`() {
        Settings
            .fromJson("""{"version": 2, "defaultZone": "device", "theme": "System", "rules": [${ruleJson("{}")}]}""")
            .shouldBeInstanceOf<SettingsDecode.Unreadable>()
        Settings
            .fromJson("""{"version": 1, "rules": [${ruleJson("""{"zone": "device"}""")}]}""")
            .shouldBeInstanceOf<SettingsDecode.Unreadable>()
    }

    @Test
    fun `a migrated document saves as version 9`() {
        val migrated =
            Settings
                .fromJson(
                    """{"version": 1, "rules": [${ruleJson("{}")}]}""",
                ).shouldBeInstanceOf<SettingsDecode.Decoded>()

        migrated.settings.toJson().startsWith("{\n    \"version\": 9,") shouldBe true
        Settings.fromJson(migrated.settings.toJson()) shouldBe migrated
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidRules")
    fun `invalid rules explain where they went wrong`(
        override: String,
        detail: String,
    ) {
        Settings.fromJson(document(rule = override)).shouldBeInstanceOf<SettingsDecode.Invalid>().detail shouldBe detail
    }

    @Test
    fun `duplicate rule names are invalid`() {
        val work = ruleJson("{}")

        Settings
            .fromJson(
                """{"version": 1, "rules": [$work, $work]}""",
            ).shouldBeInstanceOf<SettingsDecode.Invalid>()
            .detail shouldBe
            "duplicate rule names [Work]"
    }

    @Test
    fun `the first invalid rule is reported`() {
        val good = ruleJson("{}")
        val bad = ruleJson("""{"name": "Later", "policy": "Mute"}""")

        Settings.fromJson("""{"version": 1, "rules": [$good, $bad]}""") shouldBe
            SettingsDecode.Invalid("rule 2: \"Later\": unknown policy \"Mute\"")
    }

    @Test
    fun `other versions are unsupported whatever their shape`() {
        val decode = Settings.fromJson("""{"version": 10, "profiles": [], "schedules": {}}""")

        decode.shouldBeInstanceOf<SettingsDecode.UnsupportedVersion>().version shouldBe 10
        decode.shouldBeInstanceOf<SettingsDecode.Failure>().detail shouldBe "Unsupported settings version 10"
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            """{"rules": []}""",
            """{"version": "1", "rules": []}""",
            """{"version": "one", "rules": []}""",
            """{"version": {}, "rules": []}""",
        ],
    )
    fun `a missing or non-integer version is unreadable`(text: String) {
        Settings.fromJson(text).shouldBeInstanceOf<SettingsDecode.Unreadable>().detail shouldBe
            "missing or non-integer \"version\""
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["[]", "\"text\"", "1"])
    fun `a document that isn't an object is unreadable`(text: String) {
        Settings.fromJson(text).shouldBeInstanceOf<SettingsDecode.Unreadable>().detail shouldBe "not a JSON object"
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            "{nope",
            """{"version": 1}""",
            """{"version": 1, "rules": [], "extra": true}""",
            """{"version": 1, "rules": "none"}""",
        ],
    )
    fun `unparseable documents are unreadable with the parser's detail`(text: String) {
        val detail = Settings.fromJson(text).shouldBeInstanceOf<SettingsDecode.Unreadable>().detail

        detail.take("kotlinx.serialization.".length) shouldBe "kotlinx.serialization."
    }

    private fun currentDocument(
        breaks: String = "",
        chats: String = "",
        patterns: String = "",
    ): String {
        val rule =
            ruleJson(
                """{"breaks": [$breaks], "chats": [$chats], "patterns": [$patterns], "zone": "default",
                "asking": []}""",
            )

        return """{"version": 6, "defaultZone": "device", "theme": "System", "rules": [$rule]}"""
    }

    private fun narrowedApp(
        app: String,
        accounts: String = "",
        chats: String = "",
        patterns: String = "",
        asking: Boolean = false,
    ): String =
        """{"app": "$app", "scope": {"type": "narrowed", "accounts": [$accounts], "chats": [$chats],
        "patterns": [$patterns], "asking": $asking}}"""

    private fun appsDocument(
        apps: String,
        version: Int = 7,
    ): String =
        """{"version": $version, "defaultZone": "device", "theme": "System", "rules": [{"name": "Work",
        "state": "Enabled", "policy": "DeliverOnlyDuring", "windows": [$MONDAY], "breaks": [], "zone": "default",
        "apps": [$apps]}]}"""

    private fun addressedApp(
        app: String,
        addresses: String,
    ): String =
        """{"app": "$app", "scope": {"type": "narrowed", "accounts": [], "addresses": [$addresses], "chats": [],
        "patterns": [], "asking": false}}"""

    private fun document(rule: String): String = """{"version": 1, "rules": [${ruleJson(rule)}]}"""

    private fun ruleJson(override: String): String {
        val base =
            Json
                .parseToJsonElement(
                    """{"name": "Work", "state": "Enabled", "policy": "DeliverOnlyDuring",
                    "windows": [$MONDAY],
                    "apps": [], "gmailAccounts": []}""",
                ).jsonObject

        return JsonObject(base + Json.parseToJsonElement(override).jsonObject).toString()
    }
}
