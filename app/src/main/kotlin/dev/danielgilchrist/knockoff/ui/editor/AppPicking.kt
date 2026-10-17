package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import dev.danielgilchrist.knockoff.domain.notification.PackageName

sealed interface AppPicking {
    data object Closed : AppPicking

    data class Open(
        val chosen: Set<PackageName>,
    ) : AppPicking

    companion object {
        val SAVER: Saver<AppPicking, Any> =
            listSaver(
                save = { picking ->
                    when (picking) {
                        Closed -> listOf(AppPickingTag.Closed.name)
                        is Open -> listOf(AppPickingTag.Open.name) + picking.chosen.map { it.value }
                    }
                },
                restore = { saved ->
                    when (AppPickingTag.valueOf(saved.first())) {
                        AppPickingTag.Closed -> Closed
                        AppPickingTag.Open -> Open(saved.drop(1).map(::PackageName).toSet())
                    }
                },
            )
    }
}
