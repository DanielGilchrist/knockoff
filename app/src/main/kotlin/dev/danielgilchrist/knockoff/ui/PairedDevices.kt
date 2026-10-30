package dev.danielgilchrist.knockoff.ui

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import dev.danielgilchrist.knockoff.system.SystemCall
import dev.danielgilchrist.knockoff.system.systemCall

sealed interface PairedDevices {
    data object NotAllowed : PairedDevices

    data object Blocked : PairedDevices

    data object BluetoothOff : PairedDevices

    data object NoBluetooth : PairedDevices

    data class Listed(
        val devices: List<PairedDevice>,
    ) : PairedDevices

    companion object {
        fun of(
            context: Context,
            ask: PermissionAsk,
        ): PairedDevices {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter

            return when {
                adapter == null -> {
                    NoBluetooth
                }

                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) !=
                    PackageManager.PERMISSION_GRANTED -> {
                    refused(ask)
                }

                !adapter.isEnabled -> {
                    BluetoothOff
                }

                else -> {
                    when (
                        val call =
                            systemCall {
                                adapter.bondedDevices.map {
                                    PairedDevice.of(
                                        it.alias,
                                        BluetoothAddress(it.address),
                                    )
                                }
                            }
                    ) {
                        is SystemCall.Done -> listed(call.value)
                        is SystemCall.Refused -> refused(ask)
                        is SystemCall.Unavailable -> BluetoothOff
                    }
                }
            }
        }

        fun refused(ask: PermissionAsk): PairedDevices =
            when (ask) {
                PermissionAsk.Askable -> NotAllowed
                PermissionAsk.Blocked -> Blocked
            }

        fun listed(devices: List<PairedDevice>): Listed =
            Listed(devices.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }))
    }
}
