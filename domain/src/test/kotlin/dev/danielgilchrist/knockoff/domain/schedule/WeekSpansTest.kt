package dev.danielgilchrist.knockoff.domain.schedule

import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test

class WeekSpansTest {
    @Test
    fun `merging joins overlapping and touching spans`() {
        listOf(WeekSpan(5, 8), WeekSpan(0, 3), WeekSpan(3, 4), WeekSpan(6, 9)).merged() shouldBe
            listOf(WeekSpan(0, 4), WeekSpan(5, 9))
    }

    @Test
    fun `without removes cuts from every span`() {
        listOf(WeekSpan(0, 10), WeekSpan(20, 30)).without(listOf(WeekSpan(5, 25))) shouldBe
            listOf(WeekSpan(0, 5), WeekSpan(25, 30))
        WeekSpan(0, 10).without(WeekSpan(0, 10)) shouldBe emptyList()
        WeekSpan(0, 10).without(WeekSpan(20, 30)) shouldBe listOf(WeekSpan(0, 10))
    }

    @Test
    fun `only the part within the standard week is counted`() {
        val week = WeekSpan.NANOS_PER_WEEK

        listOf(WeekSpan(-10, 10), WeekSpan(week - 5, week + 5)).nanosWithinWeek() shouldBe 15
    }
}
