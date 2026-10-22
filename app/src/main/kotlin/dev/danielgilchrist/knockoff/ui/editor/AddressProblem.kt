package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress

sealed interface AddressProblem {
    data object None : AddressProblem

    sealed interface Shown : AddressProblem {
        val message: String
    }

    data object NotAnAddress : Shown {
        override val message: String get() = "Enter an email address, like name@example.com."
    }

    data class AlreadyListed(
        val address: AccountAddress,
    ) : Shown {
        override val message: String get() = "${address.value} is already listed."
    }
}
