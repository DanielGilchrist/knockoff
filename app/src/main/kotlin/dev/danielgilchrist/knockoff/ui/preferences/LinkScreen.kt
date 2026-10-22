package dev.danielgilchrist.knockoff.ui.preferences

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.ui.DeviceLink
import dev.danielgilchrist.knockoff.ui.PairedDevices
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.IconBadge
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.ListScreen
import dev.danielgilchrist.knockoff.ui.components.NoticeAction
import dev.danielgilchrist.knockoff.ui.components.NoticeCard
import dev.danielgilchrist.knockoff.ui.components.NoticeTone
import dev.danielgilchrist.knockoff.ui.components.ScreenNavigation
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
import dev.danielgilchrist.knockoff.ui.components.groupedItemColors

@Composable
internal fun LinkScreen(
    link: DeviceLink,
    paired: PairedDevices,
    actions: LinkActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)

    ListScreen(title = "Silent holds", navigation = ScreenNavigation.Back(actions.onBack), modifier = modifier) {
        status(link)
        item { SectionHeader(text = "Why Knockoff needs a link") }
        item { Paragraph(text = WHY) }
        item { SectionHeader(text = "What the link does") }
        item { LinkFacts() }
        when (link) {
            is DeviceLink.Linked -> {}

            DeviceLink.NotLinked -> {
                devicePicker(paired, actions)
            }
        }
    }
}

private const val WHY =
    "Android plays a notification's sound or vibration before Knockoff can hide it. " +
        "To keep held notifications quiet, Knockoff turns down their alerts when a hold starts. " +
        "Android only lets an app change another app's notification settings if it's linked to a " +
        "companion device, such as a watch."

private const val NEARBY_NEEDED = "Knockoff needs the Nearby devices permission to list the devices you've paired. "

private const val NEARBY_PROMPT =
    "Android's prompt says this lets apps find, connect to and work out the position of nearby devices. " +
        "Knockoff only reads the list of devices you've paired."

private const val NEARBY_BLOCKED =
    "If Android doesn't ask, turn on Nearby devices under Permissions in Knockoff's app settings."

@Composable
private fun NearbyNotice(
    detail: String,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NoticeCard(
        title = "Nearby devices",
        body = NEARBY_NEEDED + detail,
        tone = NoticeTone.Nudge,
        action = NoticeAction.Button("Show my devices", onAllow),
        modifier = modifier,
    )
}

private fun LazyListScope.status(link: DeviceLink) {
    item {
        when (link) {
            is DeviceLink.Linked -> {
                ListGroup {
                    ListItem(
                        headlineContent = { Text(text = "Linked to ${link.device}") },
                        supportingContent = {
                            Text(
                                text =
                                    "Held notifications stay quiet, apart from a chat Knockoff hasn't seen before " +
                                        "or an account an app only names in the notification.",
                            )
                        },
                        leadingContent = { IconBadge(icon = R.drawable.ic_bluetooth) },
                        trailingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle),
                                contentDescription = "Done",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        colors = groupedItemColors(),
                    )
                }
            }

            DeviceLink.NotLinked -> {
                NoticeCard(
                    title = "Not linked",
                    body = "Held notifications still buzz or ring before Knockoff hides them.",
                    tone = NoticeTone.Nudge,
                )
            }
        }
    }
}

@Composable
private fun LinkFacts(modifier: Modifier = Modifier) {
    ListGroup(modifier = modifier) {
        Fact(
            icon = R.drawable.ic_notifications,
            headline = "Turns down alerts",
            supporting =
                "Knockoff changes notification settings only for the apps and chats in your rules. " +
                    "It gives chats their own setting where Android allows, " +
                    "and turns alerts down only while they're held.",
        )
        Fact(
            icon = R.drawable.ic_bluetooth,
            headline = "Never uses the device",
            supporting =
                "Knockoff doesn't connect to it, pair with it, track it or send it anything. " +
                    "The link is only a record Android keeps, so the device can stay switched off or far away.",
        )
        Fact(
            icon = R.drawable.ic_settings,
            headline = "Comes with three Android allowances",
            supporting =
                "Linked apps may open screens from the background, aren't paused or stripped of permissions " +
                    "when unused, and may see sensitive notification content such as one-time codes, which " +
                    "Android can hide from apps like Knockoff. Knockoff doesn't open screens from the background " +
                    "or read message text.",
        )
        Fact(
            icon = R.drawable.ic_delete,
            headline = "Removing it",
            supporting =
                "Uninstalling Knockoff or clearing its storage removes the link. Do it outside held hours: " +
                    "anything held at the time stays quiet until you turn its notifications back up in Settings.",
        )
    }
}

@Composable
private fun Fact(
    @DrawableRes icon: Int,
    headline: String,
    supporting: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(text = headline) },
        supportingContent = { Text(text = supporting) },
        leadingContent = { IconBadge(icon = icon) },
        colors = groupedItemColors(),
        modifier = modifier,
    )
}

private fun LazyListScope.devicePicker(
    paired: PairedDevices,
    actions: LinkActions,
) {
    item { SectionHeader(text = "Pick one of your devices") }
    item { Paragraph(text = "Any device you've paired with this phone works, even one you no longer use.") }
    when (paired) {
        PairedDevices.NotAllowed -> {
            item { NearbyNotice(detail = NEARBY_PROMPT, onAllow = actions.system.onAllowNearbyDevices) }
        }

        PairedDevices.Blocked -> {
            item { NearbyNotice(detail = NEARBY_BLOCKED, onAllow = actions.system.onAllowNearbyDevices) }
            item {
                TextButton(onClick = actions.system.onOpenAppSettings) { Text(text = "Open app settings") }
            }
        }

        PairedDevices.BluetoothOff -> {
            item {
                NoticeCard(
                    title = "Bluetooth is off",
                    body = "Turn on Bluetooth to list the devices you've paired.",
                    tone = NoticeTone.Nudge,
                    action = NoticeAction.Button("Open Bluetooth settings", actions.system.onOpenBluetoothSettings),
                )
            }
        }

        PairedDevices.NoBluetooth -> {
            item { Paragraph(text = "This phone has no Bluetooth.") }
        }

        is PairedDevices.Listed -> {
            pairedList(paired, actions)
        }
    }
    item {
        TextButton(onClick = actions.system.onLinkNearby) { Text(text = "Pick a nearby device instead") }
    }
    item {
        Paragraph(
            text =
                "Lists Bluetooth devices and Wi-Fi networks in range, including other people's. " +
                    "Your own Wi-Fi network is a good choice. Android's list says the device will be " +
                    "managed by Knockoff, but Knockoff never uses it.",
        )
    }
}

private fun LazyListScope.pairedList(
    paired: PairedDevices.Listed,
    actions: LinkActions,
) {
    when {
        paired.devices.isEmpty() -> {
            item { Paragraph(text = "No paired devices.") }
        }

        else -> {
            item {
                ListGroup {
                    paired.devices.forEach { device ->
                        ListItem(
                            headlineContent = { Text(text = device.name) },
                            leadingContent = { IconBadge(icon = R.drawable.ic_bluetooth) },
                            colors = groupedItemColors(),
                            modifier =
                                Modifier.clickable(onClickLabel = "Link", role = Role.Button) {
                                    actions.system.onLinkPaired(device)
                                },
                        )
                    }
                }
            }
            item {
                Paragraph(
                    text =
                        "Android then asks you to confirm. Its message says linked apps can sync info " +
                            "such as callers' names with the device. Knockoff never syncs anything.",
                )
            }
        }
    }
}

@Composable
private fun Paragraph(
    text: String,
    modifier: Modifier = Modifier,
) {
    Caption(text = text, modifier = modifier.padding(horizontal = 16.dp))
}
