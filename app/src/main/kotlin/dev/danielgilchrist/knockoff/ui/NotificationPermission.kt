package dev.danielgilchrist.knockoff.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

enum class NotificationPermission {
    Granted,
    NotGranted,
    ;

    companion object {
        fun of(context: Context): NotificationPermission =
            when (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> Granted
                else -> NotGranted
            }
    }
}
