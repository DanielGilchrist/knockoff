package dev.danielgilchrist.knockoff.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.system.SystemCall
import dev.danielgilchrist.knockoff.system.systemCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

sealed interface InstalledApps {
    companion object {
        private val RETRIES = listOf(2.seconds, 10.seconds, 1.minutes)

        suspend fun load(packageManager: PackageManager): InstalledApps =
            RETRIES.fold(loadOnce(packageManager)) { last, wait ->
                when (last) {
                    is Unavailable -> {
                        delay(wait)
                        loadOnce(packageManager)
                    }

                    Loading, is Loaded -> {
                        last
                    }
                }
            }

        private suspend fun loadOnce(packageManager: PackageManager): InstalledApps =
            withContext(Dispatchers.IO) {
                when (val call = systemCall { listed(packageManager) }) {
                    is SystemCall.Done -> Loaded(call.value)
                    is SystemCall.Failed -> Unavailable(call.detail)
                }
            }

        private fun listed(packageManager: PackageManager): List<InstalledApp> {
            val launchable = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val mail =
                packageManager
                    .queryIntentActivities(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")),
                        PackageManager.ResolveInfoFlags.of(0),
                    ).map { it.activityInfo.packageName }
                    .toSet()

            return packageManager
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
                .sortedBy { it.label.lowercase() }
        }
    }

    data object Loading : InstalledApps

    data class Loaded(
        val apps: List<InstalledApp>,
    ) : InstalledApps

    data class Unavailable(
        val detail: String,
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
            Loading, is Unavailable -> whileLoading
            is Loaded -> apps.firstOrNull { it.packageName == packageName }?.let(found) ?: absent
        }
}
