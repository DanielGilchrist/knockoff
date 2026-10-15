package dev.danielgilchrist.knockoff.domain.zone

import java.time.DateTimeException
import java.time.ZoneId

sealed interface ZoneChoice {
    data object Device : ZoneChoice

    data class Fixed(
        val zone: ZoneId,
    ) : ZoneChoice

    companion object {
        private const val DEVICE = "device"

        fun parse(raw: String): ZoneChoiceParse =
            when (raw) {
                DEVICE -> {
                    ZoneChoiceParse.Valid(Device)
                }

                else -> {
                    try {
                        ZoneChoiceParse.Valid(Fixed(ZoneId.of(raw)))
                    } catch (exception: DateTimeException) {
                        ZoneChoiceParse.Unknown(raw, exception.toString())
                    }
                }
            }
    }

    fun zoneFor(device: ZoneId): ZoneId =
        when (this) {
            Device -> device
            is Fixed -> zone
        }

    fun stored(): String =
        when (this) {
            Device -> DEVICE
            is Fixed -> zone.id
        }
}
