package dev.danielgilchrist.knockoff.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf

infix fun <T> T.shouldBe(expected: T) {
    assertEquals(expected, this)
}

inline fun <reified T> Any?.shouldBeInstanceOf(): T = assertInstanceOf(T::class.java, this)
