package dev.danielgilchrist.knockoff.ui

import android.companion.CompanionDeviceManager
import android.content.Context

enum class DeviceLink {
    Linked,
    NotLinked,
    ;

    companion object {
        fun of(context: Context): DeviceLink =
            when {
                context.getSystemService(CompanionDeviceManager::class.java).myAssociations.isEmpty() -> NotLinked
                else -> Linked
            }
    }
}
