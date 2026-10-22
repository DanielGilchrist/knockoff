package dev.danielgilchrist.knockoff.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface InstalledApps {
    companion object {
        suspend fun load(packageManager: PackageManager): Loaded =
            withContext(Dispatchers.IO) {
                val launchable = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val mail =
                    packageManager
                        .queryIntentActivities(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")),
                            PackageManager.ResolveInfoFlags.of(0),
                        ).map { it.activityInfo.packageName }
                        .toSet()

                Loaded(
                    packageManager
                        .queryIntentActivities(launchable, PackageManager.ResolveInfoFlags.of(0))
                        .map {
                            InstalledApp(
                                PackageName(it.activityInfo.packageName),
                                it.loadLabel(packageManager).toString(),
                                when (it.activityInfo.packageName) {
                                    in mail -> AppPurpose.Mail
                                    else -> AppPurpose.Other
                                },
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

    fun labelOf(packageName: PackageName): String =
        lookUp(packageName, packageName.value, packageName.value) { it.label }

    fun purposeOf(packageName: PackageName): AppPurpose =
        lookUp(packageName, AppPurpose.Other, AppPurpose.Other) {
            it.purpose
        }

    fun isMissing(packageName: PackageName): Boolean =
        lookUp(packageName, whileLoading = false, absent = true) { false }

    private fun <T> lookUp(
        packageName: PackageName,
        whileLoading: T,
        absent: T,
        found: (InstalledApp) -> T,
    ): T =
        when (this) {
            Loading -> whileLoading
            is Loaded -> apps.firstOrNull { it.packageName == packageName }?.let(found) ?: absent
        }
}
