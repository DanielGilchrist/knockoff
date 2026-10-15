package dev.danielgilchrist.knockoff.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId

class ZoneDescriptionTest {
    @Test
    fun `a zone reads as its place with spaces and its current offset`() {
        assertEquals("Asia/Ho Chi Minh (UTC+07:00)", ZoneId.of("Asia/Ho_Chi_Minh").describe())
    }

    @Test
    fun `UTC shows a zero offset`() {
        assertEquals("Etc/UTC (UTC+00:00)", ZoneId.of("Etc/UTC").describe())
    }
}
