package dev.danielgilchrist.knockoff.ui.appicon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dev.danielgilchrist.knockoff.domain.notification.PackageName

object PackageChanges {
    fun register(
        context: Context,
        onChange: (PackageName) -> Unit,
    ) {
        context.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    intent.data?.schemeSpecificPart?.let { onChange(PackageName(it)) }
                }
            },
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            },
            Context.RECEIVER_NOT_EXPORTED,
        )
    }
}
