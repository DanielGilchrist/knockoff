package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstalledAppsTest {
    private val slack = PackageName("com.Slack")
    private val loaded = InstalledApps.Loaded(listOf(InstalledApp(slack, "Slack", AppPurpose.Other)))

    @Test
    fun `an app is missing only once the installed apps have loaded without it`() {
        assertEquals(false, InstalledApps.Loading.isMissing(slack))
        assertEquals(false, loaded.isMissing(slack))
        assertEquals(true, loaded.isMissing(PackageName("org.signal")))
    }

    @Test
    fun `apps are labelled by name, or by package until loaded or when unknown`() {
        assertEquals("com.Slack", InstalledApps.Loading.labelOf(slack))
        assertEquals("Slack", loaded.labelOf(slack))
        assertEquals("org.signal", loaded.labelOf(PackageName("org.signal")))
    }

    @Test
    fun `only a loaded mail app is a mail app`() {
        val proton = PackageName("ch.protonmail.android")
        val apps = InstalledApps.Loaded(listOf(InstalledApp(proton, "Proton Mail", AppPurpose.Mail)) + loaded.apps)

        assertEquals(AppPurpose.Mail, apps.purposeOf(proton))
        assertEquals(AppPurpose.Other, apps.purposeOf(slack))
        assertEquals(AppPurpose.Other, apps.purposeOf(PackageName("org.signal")))
        assertEquals(AppPurpose.Other, InstalledApps.Loading.purposeOf(proton))
    }
}
