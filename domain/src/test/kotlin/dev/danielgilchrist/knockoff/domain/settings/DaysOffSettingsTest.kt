package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.dayoff.CalendarId
import dev.danielgilchrist.knockoff.domain.dayoff.DaysOff
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.dayoff.PublicHolidays
import dev.danielgilchrist.knockoff.domain.days
import dev.danielgilchrist.knockoff.domain.leave
import dev.danielgilchrist.knockoff.domain.region
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import dev.danielgilchrist.knockoff.domain.window
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.DayOfWeek.MONDAY

class DaysOffSettingsTest {
    companion object {
        private const val MONDAY_HOURS = """{"days": ["MONDAY"], "start": "08:00", "end": "17:00"}"""
    }

    @Test
    fun `days off and a holiday calendar round-trip`() {
        val settings =
            Settings(
                ruleSet(
                    rule(
                        "Work",
                        Policy.DeliverOnlyDuring,
                        WORK_HOURS,
                        emptySet(),
                        daysOff =
                            DaysOff(
                                listOf(leave("2026-10-30", "2026-10-30"), leave("2026-12-24", "2027-01-02")),
                                PublicHolidays.Off,
                            ),
                    ),
                ),
                Theme.Dark,
                HolidayCalendar.Chosen(CalendarId(3), setOf(region("England"), region("Wales"))),
            )

        Settings.fromJson(settings.toJson()) shouldBe SettingsDecode.Decoded(settings)
    }

    @Test
    fun `a calendar with no regions round-trips`() {
        val settings = Settings(ruleSet(), Theme.System, HolidayCalendar.Chosen(CalendarId(-1), emptySet()))

        Settings.fromJson(settings.toJson()) shouldBe SettingsDecode.Decoded(settings)
    }

    @Test
    fun `version 8 documents migrate with no days off and no holiday calendar`() {
        Settings
            .fromJson(
                """{"version": 8, "defaultZone": "device", "theme": "System", "rules": [{"name": "Work",
                "state": "Enabled", "policy": "DeliverOnlyDuring", "windows": [$MONDAY_HOURS], "breaks": [],
                "zone": "default", "apps": []}]}""",
            ).shouldBeInstanceOf<SettingsDecode.Decoded>()
            .settings shouldBe
            Settings(
                ruleSet(
                    rule(
                        "Work",
                        Policy.DeliverOnlyDuring,
                        schedule(window(days(MONDAY), "08:00", "17:00")),
                        emptySet(),
                    ),
                ),
                Theme.System,
                HolidayCalendar.None,
            )
    }

    @ParameterizedTest(name = "{0} is invalid: {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            """[{"first": "30 Oct", "last": "2026-10-30"}]|Off|day off 1: "30 Oct" is not a date""",
            """[{"first": "2026-10-30", "last": "Friday"}]|Off|day off 1: "Friday" is not a date""",
            """[]|Sometimes|unknown public holidays "Sometimes"""",
        ],
    )
    fun `invalid days off explain why`(
        daysOff: String,
        publicHolidays: String,
        detail: String,
    ) {
        Settings.fromJson(document(""""daysOff": $daysOff, "publicHolidays": "$publicHolidays"""")) shouldBe
            SettingsDecode.Invalid("rule 1: \"Work\": $detail")
    }

    @Test
    fun `days off can't end before they start`() {
        Settings.fromJson(
            document(""""daysOff": [{"first": "2026-10-30", "last": "2026-10-29"}], "publicHolidays": "Off""""),
        ) shouldBe
            SettingsDecode.Invalid("rule 1: \"Work\": day off 1: ends on 2026-10-29 before it starts on 2026-10-30")
    }

    @Test
    fun `a blank holiday region is invalid`() {
        Settings.fromJson(
            """{"version": 9, "defaultZone": "device", "theme": "System",
            "holidayCalendar": {"type": "chosen", "calendar": 3, "regions": ["England", " "]}, "rules": []}""",
        ) shouldBe SettingsDecode.Invalid("holiday calendar: region 2: blank")
    }

    @Test
    fun `a version 9 document must say its holiday calendar and each rule its days off`() {
        Settings
            .fromJson("""{"version": 9, "defaultZone": "device", "theme": "System", "rules": []}""")
            .shouldBeInstanceOf<SettingsDecode.Unreadable>()
        Settings
            .fromJson(document(""""daysOff": []"""))
            .shouldBeInstanceOf<SettingsDecode.Unreadable>()
    }

    private fun document(fields: String): String =
        """{"version": 9, "defaultZone": "device", "theme": "System", "holidayCalendar": {"type": "none"}, "rules": [{"name": "Work",
        "state": "Enabled", "policy": "DeliverOnlyDuring", "windows": [$MONDAY_HOURS], "breaks": [], "zone": "default",
        "apps": [], $fields}]}"""
}
