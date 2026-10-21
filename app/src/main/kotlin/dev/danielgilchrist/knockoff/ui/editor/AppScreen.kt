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
import dev.danielgilchrist.knockoff.domain.group.GroupLabel
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.ui.DeviceLink
import dev.danielgilchrist.knockoff.ui.GroupNoun
import dev.danielgilchrist.knockoff.ui.LinkState
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
                onDraftChange = actions.onDraftChange,
            )
        }
        learningItems(
            label = label,
            kind = kind,
            offersNarrowing = draft.offersNarrowing(available),
            unnamed = choices.groups.groupsOf(app).any { it.label == GroupLabel.Unnamed },
            noun = accounts.noun,
            link = choices.link,
        )
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

private fun LazyListScope.learningItems(
    label: String,
    kind: AppKind,
    offersNarrowing: Boolean,
    unnamed: Boolean,
    noun: GroupNoun,
    link: LinkState,
) {
    if (kind == AppKind.Plain || !offersNarrowing) {
        item { Caption(text = moreCaption(label, kind, noun)) }
    }
    if (unnamed && needsLink(link)) {
        item { LinkNotice(label, noun) }
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

private fun needsLink(link: LinkState): Boolean =
    when (link) {
        LinkState.Loading -> {
            false
        }

        is LinkState.Known -> {
            when (link.link) {
                is DeviceLink.Linked -> false
                DeviceLink.NotLinked -> true
            }
        }
    }

@Composable
private fun LinkNotice(
    label: String,
    noun: GroupNoun,
) {
    NoticeCard(
        title = "Link a device to see ${noun.one} names",
        body =
            "Knockoff knows some of $label's ${noun.many} only by ID, because reading their names needs the " +
                "device link. Set it up in Settings, under Silent holds.",
        tone = NoticeTone.Nudge,
    )
}
