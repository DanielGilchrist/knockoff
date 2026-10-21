package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstalledAppsTest {
    private val slack = PackageName("com.Slack")
    private val loaded = InstalledApps.Loaded(listOf(InstalledApp(slack, "Slack")))

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
}
