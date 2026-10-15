package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.EVERY_DAY
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.days
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import dev.danielgilchrist.knockoff.domain.window
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.DayOfWeek
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.ZoneId

class SettingsTest {
    companion object {
        private const val MONDAY = """{"days": ["MONDAY"], "start": "08:00", "end": "17:00"}"""

        @JvmStatic
        fun invalidRules(): List<Arguments> =
            listOf(
                Arguments.of("""{"name": " "}""", "rule 1: blank name"),
                Arguments.of("""{"state": "Paused"}""", "rule 1: \"Work\": unknown state \"Paused\""),
                Arguments.of("""{"policy": "Mute"}""", "rule 1: \"Work\": unknown policy \"Mute\""),
                Arguments.of("""{"windows": []}""", "rule 1: \"Work\": no windows"),
                Arguments.of(
                    """{"gmailAccounts": ["dan@work.example", "dan"]}""",
                    "rule 1: \"Work\": Gmail account 2: Not an email address: \"dan\"",
                ),
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
                        setOf(Target.App(SLACK), Target.GmailInbox(WORK_ADDRESS)),
                    ),
                    rule(
                        "Nights",
                        Policy.HoldDuring,
                        schedule(window(EVERY_DAY, "22:00", "06:30"), window(days(SATURDAY, SUNDAY), "09:00", "09:00")),
                        setOf(Target.App(PackageName.GMAIL)),
                        RuleState.Disabled,
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
                    ),
                ).withDefaultZone(ZoneChoice.Fixed(ZoneId.of("Europe/London"))),
                Theme.Dark,
            )

        Settings.fromJson(settings.toJson()) shouldBe SettingsDecode.Decoded(settings)
    }

    @Test
    fun `no rules round-trips`() {
        val empty = Settings(ruleSet(), Theme.Light)

        Settings.fromJson(empty.toJson()) shouldBe SettingsDecode.Decoded(empty)
    }

    @Test
    fun `the stored document is stable`() {
        Settings.DEFAULT.toJson() shouldBe
            """
            {
                "version": 2,
                "defaultZone": "device",
                "theme": "System",
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
                        "apps": [],
                        "gmailAccounts": [],
                        "zone": "default"
                    }
                ]
            }
            """.trimIndent()
    }

    @Test
    fun `version 1 documents migrate to the device zone and system theme`() {
        Settings.fromJson("""{"version": 1, "rules": [${ruleJson("{}")}]}""") shouldBe
            SettingsDecode.Decoded(
                Settings(
                    ruleSet(
                        rule(
                            "Work",
                            Policy.DeliverOnlyDuring,
                            schedule(window(days(DayOfWeek.MONDAY), "08:00", "17:00")),
                            emptySet(),
                        ),
                    ),
                    Theme.System,
                ),
            )
    }

    @Test
    fun `unknown zones and themes are invalid`() {
        val zoned = ruleJson("""{"zone": "Mars/Base"}""")
        val unknown = "java.time.zone.ZoneRulesException: Unknown time-zone ID: Mars/Base"

        Settings.fromJson("""{"version": 2, "defaultZone": "Mars/Base", "theme": "System", "rules": []}""") shouldBe
            SettingsDecode.Invalid("unknown default zone \"Mars/Base\": $unknown")
        Settings.fromJson("""{"version": 2, "defaultZone": "device", "theme": "Neon", "rules": []}""") shouldBe
            SettingsDecode.Invalid("unknown theme \"Neon\"")
        Settings
            .fromJson("""{"version": 2, "defaultZone": "device", "theme": "System", "rules": [$zoned]}""")
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
    fun `a migrated document saves as version 2`() {
        val migrated =
            Settings
                .fromJson(
                    """{"version": 1, "rules": [${ruleJson("{}")}]}""",
                ).shouldBeInstanceOf<SettingsDecode.Decoded>()

        migrated.settings.toJson().startsWith("{\n    \"version\": 2,") shouldBe true
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
        val decode = Settings.fromJson("""{"version": 3, "profiles": [], "schedules": {}}""")

        decode.shouldBeInstanceOf<SettingsDecode.UnsupportedVersion>().version shouldBe 3
        decode.shouldBeInstanceOf<SettingsDecode.Failure>().detail shouldBe "Unsupported settings version 3"
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
