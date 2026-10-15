package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.EVERY_DAY
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.channel.AlertingImportance
import dev.danielgilchrist.knockoff.domain.channel.ChannelChange
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelPlan
import dev.danielgilchrist.knockoff.domain.channel.Importance
import dev.danielgilchrist.knockoff.domain.channel.SilencedChannels
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.schedule
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.window
import org.junit.jupiter.api.Test

class ChannelPlanTest {
    private val evening = london("2026-10-14T20:00")
    private val morning = london("2026-10-15T09:00")
    private val messages = ChannelKey(SLACK, ChannelId("messages"))
    private val calls = ChannelKey(SLACK, ChannelId("calls"))
    private val workMail = ChannelKey(PackageName.GMAIL, ChannelId("^nc_1_mail_dan@work.example"))
    private val personalMail = ChannelKey(PackageName.GMAIL, ChannelId("^nc_1_mail_dan@home.example"))
    private val whatsapp = PackageName("com.whatsapp")
    private val chats = ChannelKey(whatsapp, ChannelId("chats"))
    private val slackTarget = setOf(Target.App(SLACK))
    private val work =
        ruleSet(
            rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slackTarget + Target.GmailInbox(WORK_ADDRESS)),
        )

    @Test
    fun `a held channel that alerts is silenced and its importance saved`() {
        work.planChannels(slack(Importance.Default, Importance.High), SilencedChannels.NONE, evening) shouldBe
            ChannelPlan(
                silence =
                    listOf(
                        ChannelChange(messages, Importance.Default, Importance.Low),
                        ChannelChange(calls, Importance.High, Importance.Low),
                    ),
                restore = emptyList(),
                silenced = saved(messages to AlertingImportance.Default, calls to AlertingImportance.High),
            )
    }

    @Test
    fun `a held channel that is already quiet is left alone and not saved`() {
        work.planChannels(slack(Importance.Low, Importance.Min), SilencedChannels.NONE, evening) shouldBe
            nothing(SilencedChannels.NONE)
    }

    @Test
    fun `a silenced channel stays saved while it stays held`() {
        val silenced = saved(messages to AlertingImportance.Default)

        work.planChannels(slack(Importance.Low, Importance.Low), silenced, evening) shouldBe nothing(silenced)
    }

    @Test
    fun `a held channel the user made louder is silenced again and saved at the user's importance`() {
        val silenced = saved(messages to AlertingImportance.Default)

        work.planChannels(slack(Importance.High, Importance.Low), silenced, evening) shouldBe
            ChannelPlan(
                silence = listOf(ChannelChange(messages, Importance.High, Importance.Low)),
                restore = emptyList(),
                silenced = saved(messages to AlertingImportance.High),
            )
    }

    @Test
    fun `a held channel the user made quieter is forgotten`() {
        val silenced = saved(messages to AlertingImportance.Default, calls to AlertingImportance.High)

        work.planChannels(slack(Importance.None, Importance.Min), silenced, evening) shouldBe
            nothing(SilencedChannels.NONE)
    }

    @Test
    fun `a permitted silenced channel is restored and stays saved until the restore is seen`() {
        val silenced = saved(messages to AlertingImportance.High)
        val restoring = work.planChannels(slack(Importance.Low, Importance.Low), silenced, morning)

        restoring.restore shouldBe listOf(ChannelChange(messages, Importance.Low, Importance.High))
        restoring.silence shouldBe emptyList()
        restoring.silenced shouldBe silenced
        work.planChannels(slack(Importance.High, Importance.Low), restoring.silenced, morning) shouldBe
            nothing(SilencedChannels.NONE)
    }

    @Test
    fun `a permitted channel the user changed while it was silenced keeps the user's importance`() {
        work.planChannels(
            (slack(Importance.None, Importance.Low)),
            saved(messages to AlertingImportance.High),
            morning,
        ) shouldBe
            nothing(SilencedChannels.NONE)
    }

    @Test
    fun `a permitted channel that was never silenced is left alone`() {
        work.planChannels(slack(Importance.Default, Importance.High), SilencedChannels.NONE, morning) shouldBe
            nothing(SilencedChannels.NONE)
    }

    @Test
    fun `only the targeted account's Gmail channel is silenced`() {
        val gmail = mapOf(workMail to Importance.Default, personalMail to Importance.Default)

        work.planChannels(gmail, SilencedChannels.NONE, evening) shouldBe
            ChannelPlan(
                silence = listOf(ChannelChange(workMail, Importance.Default, Importance.Low)),
                restore = emptyList(),
                silenced = saved(workMail to AlertingImportance.Default),
            )
    }

    @Test
    fun `channels no rule targets are left alone`() {
        work.planChannels(
            mapOf(chats to Importance.High),
            SilencedChannels.NONE,
            evening,
        ) shouldBe
            nothing(SilencedChannels.NONE)
    }

    @Test
    fun `a channel one rule holds and another permits is held`() {
        val lunch = rule("Lunch", Policy.HoldDuring, schedule(window(EVERY_DAY, "12:00", "13:00")), slackTarget)

        ruleSet(work.rules.single(), lunch)
            .planChannels(
                (slack(Importance.Default, Importance.Low)),
                SilencedChannels.NONE,
                london("2026-10-14T12:30"),
            ).silence shouldBe listOf(ChannelChange(messages, Importance.Default, Importance.Low))
    }

    @Test
    fun `a channel silenced for a rule since disabled is restored`() {
        val disabled = ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, slackTarget, RuleState.Disabled))

        disabled
            .planChannels(
                (slack(Importance.Low, Importance.Default)),
                saved(messages to AlertingImportance.Default),
                evening,
            ).restore shouldBe
            listOf(ChannelChange(messages, Importance.Low, Importance.Default))
    }

    @Test
    fun `saved channels missing from a read or in unread packages are kept`() {
        val silenced = saved(chats to AlertingImportance.High, calls to AlertingImportance.Default)

        work.planChannels(emptyMap(), silenced, evening) shouldBe nothing(silenced)
    }

    @Test
    fun `channel packages are the enabled targets' packages and every silenced package`() {
        val disabled =
            rule("Off", Policy.HoldDuring, WORK_HOURS, setOf(Target.App(PackageName("com.off"))), RuleState.Disabled)

        ruleSet(work.rules.single(), disabled).channelPackages(saved(chats to AlertingImportance.High)) shouldBe
            setOf(SLACK, PackageName.GMAIL, whatsapp)
    }

    private fun slack(
        messagesImportance: Importance,
        callsImportance: Importance,
    ): Map<ChannelKey, Importance> = mapOf(messages to messagesImportance, calls to callsImportance)

    private fun saved(vararg originals: Pair<ChannelKey, AlertingImportance>): SilencedChannels =
        SilencedChannels.of(originals.toMap())

    private fun nothing(silenced: SilencedChannels): ChannelPlan =
        ChannelPlan(silence = emptyList(), restore = emptyList(), silenced = silenced)
}
