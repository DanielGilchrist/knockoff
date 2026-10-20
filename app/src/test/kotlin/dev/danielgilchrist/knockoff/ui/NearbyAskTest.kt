package dev.danielgilchrist.knockoff.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NearbyAskTest {
    @Test
    fun `a grant or a refusal Android will ask about again stays askable`() {
        assertEquals(NearbyAsk.Askable, NearbyAsk.after(granted = true, rationale = false))
        assertEquals(NearbyAsk.Askable, NearbyAsk.after(granted = true, rationale = true))
        assertEquals(NearbyAsk.Askable, NearbyAsk.after(granted = false, rationale = true))
    }

    @Test
    fun `a refusal with no rationale may be blocked`() {
        assertEquals(NearbyAsk.Blocked, NearbyAsk.after(granted = false, rationale = false))
    }
}
