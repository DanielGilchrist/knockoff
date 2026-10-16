package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.FRIEND
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test

class ChatChannelAttemptsTest {
    private val friend = COLLEAGUE_CHAT.copy(id = FRIEND, name = ConversationName("Friend"))
    private val create = setOf<ChatChannelNeed>(ChatChannelNeed.Create(COLLEAGUE_CHAT))

    @Test
    fun `each need is sorted into what to create, what is missing and what waits`() {
        val needs =
            setOf(
                ChatChannelNeed.Create(COLLEAGUE_CHAT),
                ChatChannelNeed.ParentMissing(friend),
                ChatChannelNeed.ParentSilenced(friend.copy(channel = ChannelId("group_chat_defaults_5"))),
            )

        val plan = ChatChannelAttempts.NONE.plan(needs)

        plan.create shouldBe setOf(COLLEAGUE_CHAT)
        plan.newlyMissing shouldBe setOf(friend)
        plan.waiting shouldBe setOf(friend.copy(channel = ChannelId("group_chat_defaults_5")))
    }

    @Test
    fun `a missing parent is reported once while it stays missing and again if it comes back missing`() {
        val missing = setOf<ChatChannelNeed>(ChatChannelNeed.ParentMissing(friend))
        val first = ChatChannelAttempts.NONE.plan(missing)
        val second = first.next.plan(missing)
        val recovered = second.next.plan(emptySet())

        second.newlyMissing shouldBe emptySet()
        recovered.next.plan(missing).newlyMissing shouldBe setOf(friend)
    }

    @Test
    fun `a refused chat is tried again once and then given up`() {
        val once = ChatChannelAttempts.NONE.refused(COLLEAGUE_CHAT)
        val twice = once.refused(COLLEAGUE_CHAT)

        once.plan(create).create shouldBe setOf(COLLEAGUE_CHAT)
        twice.plan(create).create shouldBe emptySet()
    }

    @Test
    fun `an unsupported chat is given up at once`() {
        ChatChannelAttempts.NONE
            .unsupported(COLLEAGUE_CHAT)
            .plan(create)
            .create shouldBe emptySet()
    }

    @Test
    fun `giving up on a chat leaves it picked again under a new parent alone`() {
        val renamed = COLLEAGUE_CHAT.copy(channel = ChannelId("individual_chat_defaults_5"))

        ChatChannelAttempts.NONE
            .unsupported(COLLEAGUE_CHAT)
            .plan(setOf(ChatChannelNeed.Create(renamed)))
            .create shouldBe setOf(renamed)
    }

    @Test
    fun `planning keeps refusals and given up chats`() {
        val refusedOnce = ChatChannelAttempts.NONE.refused(COLLEAGUE_CHAT)

        refusedOnce
            .plan(create)
            .next
            .refused(COLLEAGUE_CHAT)
            .plan(create)
            .create shouldBe emptySet()
    }
}
