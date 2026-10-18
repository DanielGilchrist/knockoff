package dev.danielgilchrist.knockoff.ui

import androidx.compose.foundation.lazy.LazyListScope
import dev.danielgilchrist.knockoff.ui.components.NoticeCard
import dev.danielgilchrist.knockoff.ui.components.NoticeTone

internal fun LazyListScope.saveProblem(saveStatus: SaveStatus) {
    when (saveStatus) {
        SaveStatus.Idle -> {}

        is SaveStatus.Failed -> {
            item {
                NoticeCard(
                    title = "Couldn't save settings",
                    body = saveStatus.detail,
                    tone = NoticeTone.Problem,
                )
            }
        }
    }
}
