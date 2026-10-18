package dev.danielgilchrist.knockoff.ui

import androidx.annotation.DrawableRes
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.rule.Policy

@get:DrawableRes
internal val Policy.icon: Int
    get() =
        when (this) {
            Policy.DeliverOnlyDuring -> R.drawable.ic_work
            Policy.HoldDuring -> R.drawable.ic_bedtime
        }
