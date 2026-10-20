package dev.danielgilchrist.knockoff.ui.preferences

import androidx.annotation.DrawableRes
import dev.danielgilchrist.knockoff.R

enum class SetupAction(
    @param:DrawableRes val icon: Int,
    val headline: String,
) {
    Access(R.drawable.ic_notifications, "Notification access"),
    Link(R.drawable.ic_watch, "Silent holds"),
    Notifications(R.drawable.ic_forum, "Knockoff's notifications"),
}
