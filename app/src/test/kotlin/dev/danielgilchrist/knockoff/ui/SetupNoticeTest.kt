package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.settings.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class SetupNoticeTest {
    private val work =
        Settings.DEFAULT.rules.rules
            .single()
    private val ready =
        Setup(ListenerAccess.Granted, DeviceLink.NotLinked, ExactAlarms.NotAllowed, NotificationPermission.NotGranted)

    @Test
    fun `missing access comes first and optional setup never shows`() {
        assertEquals(SetupNotice.NoAccess, SetupNotice.of(ready.copy(access = ListenerAccess.NotGranted), asking()))
        assertEquals(SetupNotice.Clear, SetupNotice.of(ready, Settings.DEFAULT.rules))
    }

    @Test
    fun `notifications are needed only while an enabled rule asks`() {
        assertEquals(SetupNotice.NoNotifications, SetupNotice.of(ready, asking()))
        assertEquals(SetupNotice.Clear, SetupNotice.of(ready, asking(RuleState.Disabled)))
        assertEquals(
            SetupNotice.Clear,
            SetupNotice.of(ready.copy(notifications = NotificationPermission.Granted), asking()),
        )
    }

    private fun asking(state: RuleState = RuleState.Enabled): RuleSet =
        assertInstanceOf(
            RuleSetParse.Valid::class.java,
            Settings.DEFAULT.rules.withRules(
                listOf(work.copy(state = state, asking = setOf(COLLEAGUE_CHAT.packageName))),
            ),
        ).rules
}
