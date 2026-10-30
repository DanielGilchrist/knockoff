package dev.danielgilchrist.knockoff.domain.dayoff

import dev.danielgilchrist.knockoff.domain.leave
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LeaveTest {
    private val christmas = leave("2026-12-24", "2026-12-28")

    @Test
    fun `leave can't end before it starts`() {
        Leave.parse(date("2026-12-24"), date("2026-12-23")) shouldBe LeaveParse.EndsBeforeStart
        Leave.parse(date("2026-12-24"), date("2026-12-24")) shouldBe LeaveParse.Valid(Leave.day(date("2026-12-24")))
    }

    @Test
    fun `leave covers its first and last days and those between`() {
        (date("2026-12-23") in christmas) shouldBe false
        (date("2026-12-24") in christmas) shouldBe true
        (date("2026-12-28") in christmas) shouldBe true
        (date("2026-12-29") in christmas) shouldBe false
        christmas.dates shouldBe (24..28).map { date("2026-12-$it") }
    }

    @Test
    fun `leave ends before the day after its last`() {
        christmas.endsBefore(date("2026-12-28")) shouldBe false
        christmas.endsBefore(date("2026-12-29")) shouldBe true
    }

    @Test
    fun `leaving out a day splits the leave around it`() {
        christmas.without(date("2026-12-26")) shouldBe
            listOf(leave("2026-12-24", "2026-12-25"), leave("2026-12-27", "2026-12-28"))
        christmas.without(date("2026-12-24")) shouldBe listOf(leave("2026-12-25", "2026-12-28"))
        christmas.without(date("2026-12-28")) shouldBe listOf(leave("2026-12-24", "2026-12-27"))
        christmas.without(date("2026-12-29")) shouldBe listOf(christmas)
        Leave.day(date("2026-12-24")).without(date("2026-12-24")) shouldBe emptyList()
    }

    private fun date(text: String): LocalDate = LocalDate.parse(text)
}
