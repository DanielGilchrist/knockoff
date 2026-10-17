package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.runtime.saveable.SaverScope
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class AppPickingTest {
    companion object {
        @JvmStatic
        fun states(): List<AppPicking> =
            listOf(
                AppPicking.Closed,
                AppPicking.Open(emptySet()),
                AppPicking.Open(setOf(PackageName("com.whatsapp"), PackageName("com.Slack"))),
            )
    }

    @ParameterizedTest
    @MethodSource("states")
    fun `picking survives being saved and restored`(state: AppPicking) {
        val saved = with(AppPicking.SAVER) { SaverScope { true }.save(state) }

        assertEquals(state, saved?.let(AppPicking.SAVER::restore))
    }
}
