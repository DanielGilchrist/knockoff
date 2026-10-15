package dev.danielgilchrist.knockoff.domain.notification

import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class NotificationTraitsTest {
    @Test
    fun `no traits parse as none`() {
        NotificationTraits.parse(emptySet()) shouldBe NotificationTraitsParse.None
    }

    @Test
    fun `traits parse as present`() {
        val traits = setOf(NotificationTrait.Ongoing, NotificationTrait.MediaSession)

        NotificationTraits
            .parse(traits)
            .shouldBeInstanceOf<NotificationTraitsParse.Present>()
            .traits.values shouldBe
            traits
    }

    @Test
    fun `parsed traits do not follow later changes to the given set`() {
        val given = mutableSetOf(NotificationTrait.Ongoing)
        val parse = NotificationTraits.parse(given).shouldBeInstanceOf<NotificationTraitsParse.Present>()

        given.clear()

        parse.traits.values shouldBe setOf(NotificationTrait.Ongoing)
    }
}
