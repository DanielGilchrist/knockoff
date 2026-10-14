package dev.danielgilchrist.knockoff.domain.schedule

import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class WeekDaysTest {
    @Test
    fun `parsing no days is empty`() {
        WeekDays.parse(emptySet()) shouldBe WeekDaysParse.Empty
    }

    @Test
    fun `parsed days contain exactly what was given`() {
        val parse = WeekDays.parse(setOf(DayOfWeek.TUESDAY)).shouldBeInstanceOf<WeekDaysParse.Valid>()

        parse.days.days shouldBe setOf(DayOfWeek.TUESDAY)
        (DayOfWeek.TUESDAY in parse.days) shouldBe true
        (DayOfWeek.MONDAY in parse.days) shouldBe false
    }

    @Test
    fun `parsed days do not follow later changes to the given set`() {
        val given = mutableSetOf(DayOfWeek.SUNDAY)
        val parse = WeekDays.parse(given).shouldBeInstanceOf<WeekDaysParse.Valid>()

        given.clear()

        parse.days.days shouldBe setOf(DayOfWeek.SUNDAY)
    }
}
