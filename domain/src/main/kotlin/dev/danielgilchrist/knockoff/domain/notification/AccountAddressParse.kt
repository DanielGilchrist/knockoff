package dev.danielgilchrist.knockoff.domain.notification

sealed interface AccountAddressParse {
    data class Valid(
        val address: AccountAddress,
    ) : AccountAddressParse

    data object NotAnAddress : AccountAddressParse
}
