package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.settings.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppFocusTest {
    private val rule =
        Settings.DEFAULT.rules.rules
            .single()
    private val gmail = AppFocus.App(PackageName.GMAIL)

    @Test
    fun `only a section or the wizard can focus an app`() {
        assertEquals(AppFocus.List, AppFocus.of(Editing.Closed))
        assertEquals(AppFocus.List, AppFocus.of(Editing.Viewing(rule)))
        assertEquals(AppFocus.List, AppFocus.of(Editing.Renaming.of(rule)))
        assertEquals(gmail, AppFocus.of(Editing.Section.of(rule, RuleSection.Apps).focusing(gmail)))
        assertEquals(gmail, AppFocus.of(Editing.Guided.START.focusing(gmail)))
    }
}
