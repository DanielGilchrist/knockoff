package dev.danielgilchrist.knockoff.domain.notification

enum class NotificationCategory {
    Email,
    Other,
    ;

    companion object {
        private const val EMAIL = "email"

        fun of(raw: String?): NotificationCategory =
            when (raw) {
                EMAIL -> Email
                else -> Other
            }
    }
}
