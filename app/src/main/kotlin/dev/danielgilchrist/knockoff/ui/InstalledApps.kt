package dev.danielgilchrist.knockoff.ui

import android.content.Intent
import android.content.pm.PackageManager
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface InstalledApps {
    companion object {
        suspend fun load(packageManager: PackageManager): Loaded =
            withContext(Dispatchers.IO) {
                val launchable = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

                Loaded(
                    packageManager
                        .queryIntentActivities(launchable, PackageManager.ResolveInfoFlags.of(0))
                        .map {
                            InstalledApp(
                                PackageName(it.activityInfo.packageName),
                                it.loadLabel(packageManager).toString(),
                            )
                        }.distinctBy { it.packageName }
                        .sortedBy { it.label.lowercase() },
                )
            }
    }

    data object Loading : InstalledApps

    data class Loaded(
        val apps: List<InstalledApp>,
    ) : InstalledApps
}
