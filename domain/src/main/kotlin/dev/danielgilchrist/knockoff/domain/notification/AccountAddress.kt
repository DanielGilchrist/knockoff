package dev.danielgilchrist.knockoff.domain.notification

@JvmInline
value class AccountAddress private constructor(
    val value: String,
) {
    companion object {
        private val SHAPE = Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")

        fun parse(raw: String): AccountAddressParse {
            val trimmed = raw.trim()

            return when {
                SHAPE.matches(trimmed) -> AccountAddressParse.Valid(AccountAddress(trimmed.lowercase()))
                else -> AccountAddressParse.NotAnAddress
            }
        }
    }
}
