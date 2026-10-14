package dev.danielgilchrist.knockoff.domain.notification

@JvmInline
value class ChannelId(
    val value: String,
) {
    fun endsWithAccount(address: AccountAddress): Boolean = value.endsWith("_${address.value}", ignoreCase = true)
}
