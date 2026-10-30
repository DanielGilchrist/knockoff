package dev.danielgilchrist.knockoff.ui.zone

import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.ui.describe
import java.time.ZoneId

private const val DEVICE = "Same as your phone"
private const val APP_DEFAULT = "Default"

internal val DEVICE_ZONE: LeadingZone<ZoneChoice> =
    LeadingZone(ZoneChoice.Device, DEVICE, "Follows the phone when you travel")

internal val RULE_ZONES: List<LeadingZone<RuleZone>> =
    listOf(
        LeadingZone(RuleZone.Default, APP_DEFAULT, "Follows the default in Settings"),
        LeadingZone(RuleZone.Chosen(ZoneChoice.Device), DEVICE, DEVICE_ZONE.explanation),
    )

internal fun ZoneChoice.label(): String =
    when (this) {
        ZoneChoice.Device -> DEVICE
        is ZoneChoice.Fixed -> zone.describe()
    }

internal fun RuleZone.label(
    default: ZoneChoice,
    device: ZoneId,
): String =
    when (this) {
        RuleZone.Default -> default.defaultLabel(device)
        is RuleZone.Chosen -> choice.label()
    }

private fun ZoneChoice.defaultLabel(device: ZoneId): String =
    when (this) {
        ZoneChoice.Device -> "$DEVICE (${device.id})"
        is ZoneChoice.Fixed -> "$APP_DEFAULT (${label()})"
    }
