package dev.danielgilchrist.knockoff.ui

import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.content.Context
import dev.danielgilchrist.knockoff.system.SystemCall
import dev.danielgilchrist.knockoff.system.systemCall

sealed interface DeviceLink {
    data class Linked(
        val device: String,
    ) : DeviceLink

    data object NotLinked : DeviceLink

    data class Unknown(
        val detail: String,
    ) : DeviceLink

    companion object {
        private const val UNNAMED = "a device"

        fun of(context: Context): DeviceLink =
            when (
                val call =
                    systemCall {
                        context
                            .getSystemService(
                                CompanionDeviceManager::class.java,
                            ).myAssociations
                    }
            ) {
                is SystemCall.Done -> of(call.value.firstOrNull())
                is SystemCall.Failed -> Unknown(call.detail)
            }

        private fun of(association: AssociationInfo?): DeviceLink =
            when (association) {
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
