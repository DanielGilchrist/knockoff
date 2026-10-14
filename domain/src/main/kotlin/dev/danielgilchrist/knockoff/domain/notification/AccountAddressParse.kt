package dev.danielgilchrist.knockoff.domain.notification

sealed interface AccountAddressParse {
    data class Valid(
        val address: AccountAddress,
    ) : AccountAddressParse

    data class Invalid(
        val detail: String,
    ) : AccountAddressParse
}
