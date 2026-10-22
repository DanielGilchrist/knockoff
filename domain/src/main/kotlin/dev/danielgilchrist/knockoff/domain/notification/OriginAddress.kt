package dev.danielgilchrist.knockoff.domain.notification

sealed interface OriginAddress {
    data class Addressed(
        val address: AccountAddress,
    ) : OriginAddress

    data object Unaddressed : OriginAddress

    companion object {
        fun of(subText: String?): OriginAddress =
            when (subText) {
                null -> {
                    Unaddressed
                }

                else -> {
                    when (val parse = AccountAddress.parse(subText)) {
                        is AccountAddressParse.Valid -> Addressed(parse.address)
                        AccountAddressParse.NotAnAddress -> Unaddressed
                    }
                }
            }
    }
}
