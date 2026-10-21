package dev.danielgilchrist.knockoff.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.ui.TargetChoices
import dev.danielgilchrist.knockoff.ui.appEffect
import dev.danielgilchrist.knockoff.ui.components.AddButton
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.DestructiveMenuItem
import dev.danielgilchrist.knockoff.ui.components.ListScreen
import dev.danielgilchrist.knockoff.ui.components.NoticeCard
import dev.danielgilchrist.knockoff.ui.components.NoticeTone
import dev.danielgilchrist.knockoff.ui.components.OverflowMenu
import dev.danielgilchrist.knockoff.ui.components.ScreenBar
import dev.danielgilchrist.knockoff.ui.components.ScreenNavigation
import dev.danielgilchrist.knockoff.ui.ruleCalled

@Composable
internal fun AppScreen(
    app: PackageName,
    draft: AppDraft,
    rule: RuleDraft,
    stored: RuleApps,
    choices: TargetChoices,
    actions: AppScreenActions,
    modifier: Modifier = Modifier,
) {
    val label = choices.apps.labelOf(app)
    val (kind, accounts) = AppKind.forApp(app, draft, stored, choices)
    val available = accounts.offered.size + AppKind.seenChatsOf(app, choices.chats).size
    val discovery = choices.discoveries.of(app)
    val effect = rule.appEffect(appHeld(label, draft.mode, kind, accounts.noun))
    var patterning by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = actions.onBack)

    ListScreen(
        title = label,
        navigation = ScreenNavigation.Back(actions.onBack),
        bar = ScreenBar.Small,
        actions = { RemoveMenu(ruleName = rule.name, onRemove = actions.onRemove) },
        modifier = modifier,
    ) {
        if (effect.isNotEmpty()) item { Caption(text = effect) }
        item {
            IncludeChoice(
                app = app,
                label = label,
                rule = rule,
                draft = draft,
                kind = kind,
                accounts = accounts,
                seen = choices.chats,
                available = available,
                discovery = discovery,
                onDraftChange = actions.onDraftChange,
            )
        }
        val note =
            DiscoveryNote.of(
                DiscoveryFacts(label, kind, accounts),
                discovery,
                choices.link,
            )

        noteItem(note)
        if (note == DiscoveryNote.None && (kind == AppKind.Plain || !draft.offersNarrowing(available))) {
            item { Caption(text = moreCaption(label, kind, accounts.noun)) }
        }
        patternItems(
            app = app,
            draft = draft,
            kind = kind,
            choices = choices,
            shown = patterning || draft.patterns.isNotEmpty(),
            onReveal = {
                patterning = true
                actions.onDraftChange(draft.copy(mode = AppMode.Narrowed))
            },
            onDraftChange = actions.onDraftChange,
        )
    }
}

@Composable
private fun RemoveMenu(
    ruleName: String,
    onRemove: () -> Unit,
) {
    OverflowMenu { close ->
        DestructiveMenuItem(
            text = "Remove from ${ruleCalled(ruleName)}",
            onClick = {
                close()
                onRemove()
            },
        )
    }
}

private fun LazyListScope.noteItem(note: DiscoveryNote) {
    when (note) {
        DiscoveryNote.None -> {}

        is DiscoveryNote.Text -> {
            item { Caption(text = note.text) }
        }

        is DiscoveryNote.LinkNeeded -> {
            item { NoticeCard(title = note.title, body = note.body, tone = NoticeTone.Nudge) }
        }
    }
}

private fun LazyListScope.patternItems(
    app: PackageName,
    draft: AppDraft,
    kind: AppKind,
    choices: TargetChoices,
    shown: Boolean,
    onReveal: () -> Unit,
    onDraftChange: (AppDraft) -> Unit,
) {
    val narrowed = draft.mode == AppMode.Narrowed
    val editing = shown && narrowed

    item {
        Column {
            AnimatedVisibility(visible = editing, enter = expandVertically(), exit = shrinkVertically()) {
                PatternsEditor(
                    app = app,
                    seen = choices.chats,
                    patterns = draft.patterns,
                    matchesChats = kind.hasChats,
                    onPatternsChange = { onDraftChange(draft.copy(patterns = it)) },
                )
            }
            if (!editing && (narrowed || draft.patterns.isNotEmpty())) {
                AddButton(text = "Match by pattern", onClick = onReveal)
            }
        }
    }
}
