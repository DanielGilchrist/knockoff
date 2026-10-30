package dev.danielgilchrist.knockoff.system

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SystemCallsTest {
    @Test
    fun `a call that works is done with its value`() {
        assertEquals(SystemCall.Done(3), systemCall { 3 })
    }

    @Test
    fun `a refusal becomes a value`() {
        assertEquals(
            SystemCall.Refused("java.lang.SecurityException: not allowed"),
            systemCall {
                throw SecurityException("not allowed")
            },
        )
    }

    @Test
    fun `anything that isn't a binder failure still crashes`() {
        assertThrows<IllegalStateException> { systemCall { throw IllegalStateException("bug") } }
    }
}
