package dev.danielgilchrist.knockoff.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PermissionAskTest {
    @Test
    fun `a grant or a refusal Android will ask about again stays askable`() {
        assertEquals(PermissionAsk.Askable, PermissionAsk.after(granted = true, rationale = false))
        assertEquals(PermissionAsk.Askable, PermissionAsk.after(granted = true, rationale = true))
        assertEquals(PermissionAsk.Askable, PermissionAsk.after(granted = false, rationale = true))
    }

    @Test
    fun `a refusal with no rationale may be blocked`() {
        assertEquals(PermissionAsk.Blocked, PermissionAsk.after(granted = false, rationale = false))
    }
}
