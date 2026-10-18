package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.ui.SetupNotice
import dev.danielgilchrist.knockoff.ui.SystemActions
import dev.danielgilchrist.knockoff.ui.components.NoticeAction
import dev.danielgilchrist.knockoff.ui.components.NoticeCard
import dev.danielgilchrist.knockoff.ui.components.NoticeTone

@Composable
internal fun SetupBanner(
    notice: SetupNotice,
    system: SystemActions,
    modifier: Modifier = Modifier,
) {
    when (notice) {
        SetupNotice.Clear -> {}

        SetupNotice.NoAccess -> {
            NoticeCard(
                title = "Notification access is off",
                body = "Nothing is held until Knockoff can see notifications.",
                tone = NoticeTone.Problem,
                action = NoticeAction.Button("Turn on", system.onOpenAccessSettings),
                modifier = modifier,
            )
        }

        SetupNotice.NoNotifications -> {
            NoticeCard(
                title = "Allow Knockoff's notifications",
                body = "A rule asks about new chats, but Knockoff can't post notifications.",
                tone = NoticeTone.Nudge,
                action = NoticeAction.Button("Allow", system.onAllowNotifications),
                modifier = modifier,
            )
        }
    }
}
