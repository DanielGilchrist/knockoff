package dev.danielgilchrist.knockoff.channel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettlementTest {
    private val unsettled = Settlement.Unsettled("unconfirmed")
    private val busy = Settlement.Busy("binder full")

    @Test
    fun `a busy system wins so the pass is retried soon, then the first problem`() {
        assertEquals(Settlement.Settled, Settlement.Settled.and(Settlement.Settled))
        assertEquals(unsettled, Settlement.Settled.and(unsettled))
        assertEquals(busy, Settlement.Settled.and(busy))
        assertEquals(unsettled, unsettled.and(Settlement.Settled))
        assertEquals(unsettled, unsettled.and(Settlement.Unsettled("later")))
        assertEquals(busy, unsettled.and(busy))
        assertEquals(busy, busy.and(Settlement.Settled))
        assertEquals(busy, busy.and(unsettled))
        assertEquals(busy, busy.and(Settlement.Busy("later")))
    }
}
