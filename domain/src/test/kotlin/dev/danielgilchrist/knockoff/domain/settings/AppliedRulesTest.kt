package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import java.time.ZoneId

class AppliedRulesTest {
    private val london = ZoneId.of("Europe/London")
    private val paris = ZoneId.of("Europe/Paris")
    private val applied = AppliedRules(Settings.DEFAULT.rules, london)
    private val settings =
        JsonPrimitive(
            Settings(Settings.DEFAULT.rules, Theme.System, HolidayCalendar.None).toJson(),
        ).toString()

    @Test
    fun `a baseline round-trips with the rules applied since it`() {
        val baseline = Baseline.Known(applied, listOf(AppliedRules(Settings.DEFAULT.rules, paris)))

        AppliedRules.fromJson(AppliedRules.toJson(baseline)) shouldBe AppliedRulesDecode.Decoded(baseline)
    }

    @Test
    fun `a version 1 file is a reconciled baseline with nothing since`() {
        AppliedRules.fromJson("""{"version": 1, "settings": $settings, "zone": "Europe/London"}""") shouldBe
            AppliedRulesDecode.Decoded(Baseline.Known.of(applied))
    }

    @Test
    fun `applied rules evaluate in their own zone`() {
        applied.evaluation(SeenChats.NONE, Holidays.NONE) shouldBe
            Evaluation(Settings.DEFAULT.rules, london, Holidays.NONE)
    }

    @Test
    fun `an unsupported version is refused`() {
        AppliedRules.fromJson("""{"version": 9}""") shouldBe AppliedRulesDecode.UnsupportedVersion(9)
        AppliedRulesDecode.UnsupportedVersion(9).detail shouldBe "Unsupported applied rules version 9"
        AppliedRulesDecode.UnsupportedVersion(9).version shouldBe 9
    }

    @Test
    fun `an unknown or device zone is unreadable, wherever it is`() {
        listOf("Not/AZone", "device").forEach { zone ->
            AppliedRules
                .fromJson("""{"version": 1, "settings": $settings, "zone": "$zone"}""")
                .shouldBeInstanceOf<AppliedRulesDecode.Unreadable>()
        }

        val good = """{"settings": $settings, "zone": "Europe/London"}"""
        val bad = """{"settings": $settings, "zone": "Not/AZone"}"""

        AppliedRules
            .fromJson("""{"version": 2, "reconciled": $bad, "since": []}""")
            .shouldBeInstanceOf<AppliedRulesDecode.Unreadable>()
            .detail
            .startsWith("reconciled: unknown zone Not/AZone") shouldBe true
        AppliedRules
            .fromJson("""{"version": 2, "reconciled": $good, "since": [$good, $bad]}""")
            .shouldBeInstanceOf<AppliedRulesDecode.Unreadable>()
            .detail
            .startsWith("since 2: unknown zone Not/AZone") shouldBe true
    }

    @Test
    fun `unreadable embedded settings are unreadable`() {
        AppliedRules
            .fromJson("""{"version": 1, "settings": "nope", "zone": "Europe/London"}""")
            .shouldBeInstanceOf<AppliedRulesDecode.Unreadable>()
    }
}
