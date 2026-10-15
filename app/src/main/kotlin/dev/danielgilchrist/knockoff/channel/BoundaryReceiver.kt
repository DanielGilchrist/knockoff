package dev.danielgilchrist.knockoff.channel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.danielgilchrist.knockoff.KnockoffApplication

class BoundaryReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        (context.applicationContext as KnockoffApplication).boundaryReached()
    }
}
