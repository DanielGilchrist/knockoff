package dev.danielgilchrist.knockoff.ui

import android.companion.CompanionDeviceManager
import android.content.Context

sealed interface DeviceLink {
    data class Linked(
        val device: String,
    ) : DeviceLink

    data object NotLinked : DeviceLink

    companion object {
        private const val UNNAMED = "a device"

        fun of(context: Context): DeviceLink =
            when (
                val association =
                    context.getSystemService(CompanionDeviceManager::class.java).myAssociations.firstOrNull()
            ) {
                null -> {
                    NotLinked
                }

                else -> {
                    linked(
                        association.displayName?.toString(),
                        association.deviceMacAddress?.let {
                            BluetoothAddress(it.toString())
                        },
                    )
                }
            }

        fun linked(
            name: String?,
            address: BluetoothAddress?,
        ): Linked =
            Linked(
                name?.takeIf { it.isNotBlank() }
                    ?: address?.value?.uppercase()
                    ?: UNNAMED,
            )
    }
}
