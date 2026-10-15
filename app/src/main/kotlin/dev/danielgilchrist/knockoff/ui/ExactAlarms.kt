package dev.danielgilchrist.knockoff.ui

import android.app.AlarmManager
import android.content.Context

enum class ExactAlarms {
    Allowed,
    NotAllowed,
    ;

    companion object {
        fun of(context: Context): ExactAlarms =
            when {
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() -> Allowed
                else -> NotAllowed
            }
    }
}
