package dev.danielgilchrist.knockoff.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows

infix fun <T> T.shouldBe(expected: T) {
    assertEquals(expected, this)
}

inline fun <reified T> Any?.shouldBeInstanceOf(): T = assertInstanceOf(T::class.java, this)

inline fun <reified T : Throwable> shouldThrow(noinline block: () -> Unit): T = assertThrows(T::class.java, block)
