package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.ui.AppPurpose
import dev.danielgilchrist.knockoff.ui.GroupNoun

enum class AddressOffer {
    Shown,
    Hidden,
    ;

    companion object {
        fun of(
            draft: AppDraft,
            available: Int,
            purpose: AppPurpose,
            noun: GroupNoun,
        ): AddressOffer =
            when (draft.mode) {
                AppMode.Narrowed -> narrowed(purpose, noun)
                AppMode.Everything -> unnarrowed(draft, available, purpose)
            }

        private fun narrowed(
            purpose: AppPurpose,
            noun: GroupNoun,
        ): AddressOffer =
            when (purpose) {
                AppPurpose.Mail -> {
                    Shown
                }

                AppPurpose.Other -> {
                    when (noun) {
                        GroupNoun.Accounts, GroupNoun.AccountsAndCategories -> Shown
                        GroupNoun.Categories -> Hidden
                    }
                }
            }

        private fun unnarrowed(
            draft: AppDraft,
            available: Int,
            purpose: AppPurpose,
        ): AddressOffer =
            when {
                draft.offersNarrowing(available) -> {
                    Hidden
                }

                else -> {
                    when (purpose) {
                        AppPurpose.Mail -> Shown
                        AppPurpose.Other -> Hidden
                    }
                }
            }
    }
}
