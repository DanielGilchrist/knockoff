package dev.danielgilchrist.knockoff.ask

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.chat.ChatDecisionsLoad
import dev.danielgilchrist.knockoff.chat.ChatDecisionsStore
import dev.danielgilchrist.knockoff.domain.chat.ChatAnswer
import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import dev.danielgilchrist.knockoff.settings.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ChatAnswersTest {
    @TempDir
    lateinit var directory: File

    private val work =
        Settings.DEFAULT.rules.rules
            .single()

    private val settings by lazy { SettingsStore(File(directory, "settings.json")) }
    private val decisions by lazy { ChatDecisionsStore(File(directory, "chat-decisions.json")) }
    private val answers by lazy { ChatAnswers(settings, decisions) }

    @Test
    fun `holding adds the chat to the rule`() =
        runTest {
            assertEquals(AnswerOutcome.Saved, answers.answer(ChatAnswer.Hold(work.name, COLLEAGUE_CHAT)))

            val loaded = assertInstanceOf(SettingsLoad.Loaded::class.java, settings.loads.first())
            assertEquals(
                listOf(COLLEAGUE_CHAT),
                loaded.settings.rules.rules
                    .single()
                    .chats,
            )
        }

    @Test
    fun `letting through and dismissing are remembered`() =
        runTest {
            assertEquals(AnswerOutcome.Saved, answers.answer(ChatAnswer.LetThrough(COLLEAGUE_CHAT.key)))
            assertEquals(AnswerOutcome.Saved, answers.answer(ChatAnswer.Dismiss(setOf(COLLEAGUE_CHAT.key))))

            assertEquals(
                ChatDecisionsLoad.Loaded(
                    ChatDecisions.NONE.lettingThrough(COLLEAGUE_CHAT.key).askedAbout(setOf(COLLEAGUE_CHAT.key)),
                ),
                decisions.loads.first(),
            )
        }

    @Test
    fun `failures are reported`() =
        runTest {
            File(directory, "settings.json").writeText("not json")
            File(directory, "chat-decisions.json").apply {
                writeText("{}")
                setReadable(false)
            }

            assertInstanceOf(
                AnswerOutcome.Failed::class.java,
                answers.answer(ChatAnswer.Hold(work.name, COLLEAGUE_CHAT)),
            )
            assertInstanceOf(
                AnswerOutcome.Failed::class.java,
                answers.answer(ChatAnswer.LetThrough(COLLEAGUE_CHAT.key)),
            )
            assertInstanceOf(AnswerOutcome.Failed::class.java, answers.answer(ChatAnswer.Dismiss(emptySet())))
        }
}
