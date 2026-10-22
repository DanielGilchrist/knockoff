package dev.danielgilchrist.knockoff.domain.notification

import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test

class NotificationCategoryTest {
    @Test
    fun `only the email category is email`() {
        NotificationCategory.of("email") shouldBe NotificationCategory.Email
        NotificationCategory.of("msg") shouldBe NotificationCategory.Other
        NotificationCategory.of(null) shouldBe NotificationCategory.Other
    }
}
