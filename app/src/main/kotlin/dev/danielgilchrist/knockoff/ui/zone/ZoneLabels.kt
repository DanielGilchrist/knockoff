package dev.danielgilchrist.knockoff.ui.zone

import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.ui.describe

private const val DEVICE = "Device time zone"
private const val APP_DEFAULT = "App default"

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

internal fun RuleZone.label(): String =
    when (this) {
        RuleZone.Default -> APP_DEFAULT
        is RuleZone.Chosen -> choice.label()
    }
