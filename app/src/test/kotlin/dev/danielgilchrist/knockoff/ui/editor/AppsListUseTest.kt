package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.rule.Policy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppsListUseTest {
    private val work = RuleDraft.NEW.copy(name = "Work")
    private val quiet = work.copy(name = "Nights", policy = Policy.HoldDuring)

    @Test
    fun `the editor names the rule's hours and the wizard points at the next step`() {
        assertEquals("These apps only reach you during Work's hours.", AppsListUse.Editor.caption(work))
        assertEquals("These apps go quiet during Nights' hours.", AppsListUse.Editor.caption(quiet))
        assertEquals("They'll only reach you during the hours you set next.", AppsListUse.Wizard.caption(work))
        assertEquals("They'll go quiet during the hours you set next.", AppsListUse.Wizard.caption(quiet))
    }
}
