package dev.danielgilchrist.knockoff.domain.notification

@JvmInline
value class AccountAddress private constructor(
    val value: String,
) {
    companion object {
        private val SHAPE = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        fun parse(raw: String): AccountAddressParse {
            val normalised = raw.trim().lowercase()

            return when {
                SHAPE.matches(normalised) -> AccountAddressParse.Valid(AccountAddress(normalised))
                else -> AccountAddressParse.Invalid(detail = "Not an email address: \"$raw\"")
            }
        }
    }
}
