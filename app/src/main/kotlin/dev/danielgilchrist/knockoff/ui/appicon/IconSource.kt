package dev.danielgilchrist.knockoff.ui.appicon

fun interface IconSource {
    suspend fun load(key: IconKey): AppIcon.Settled
}
