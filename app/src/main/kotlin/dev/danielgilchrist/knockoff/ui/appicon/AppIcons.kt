package dev.danielgilchrist.knockoff.ui.appicon

import androidx.compose.ui.graphics.ImageBitmap
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppIcons(
    private val scope: CoroutineScope,
    private val source: IconSource,
    private val capacity: Int = CAPACITY,
) {
    private val lock = Any()
    private val loaded =
        object : LinkedHashMap<IconKey, ImageBitmap>(capacity, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<IconKey, ImageBitmap>): Boolean =
                size > capacity
        }
    private val inFlight = mutableMapOf<IconKey, Deferred<AppIcon.Settled>>()
    private val mutableGenerations = MutableStateFlow(IconGenerations.NONE)

    val generations: StateFlow<IconGenerations> = mutableGenerations.asStateFlow()

    fun current(key: IconKey): AppIcon = synchronized(lock) { loaded[key] }?.let(AppIcon::Loaded) ?: AppIcon.Loading

    suspend fun load(key: IconKey): AppIcon.Settled =
        when (val pending = pendingFor(key)) {
            is Pending.Cached -> pending.icon
            is Pending.Loading -> pending.load.also(Deferred<AppIcon.Settled>::start).await()
        }

    fun forget(app: PackageName) {
        synchronized(lock) {
            loaded.keys.removeAll { it.app == app }
            inFlight.keys.removeAll { it.app == app }
            mutableGenerations.value = mutableGenerations.value.bumped(app)
        }
    }

    private fun pendingFor(key: IconKey): Pending =
        synchronized(lock) {
            when (val image = loaded[key]) {
                null -> {
                    val generation = mutableGenerations.value.of(key.app)

                    Pending.Loading(
                        inFlight.getOrPut(key) { scope.async(start = CoroutineStart.LAZY) { fetch(key, generation) } },
                    )
                }

                else -> {
                    Pending.Cached(AppIcon.Loaded(image))
                }
            }
        }

    private suspend fun fetch(
        key: IconKey,
        generation: Int,
    ): AppIcon.Settled =
        source.load(key).also { icon ->
            synchronized(lock) {
                if (mutableGenerations.value.of(key.app) == generation) {
                    inFlight.remove(key)
                    when (icon) {
                        is AppIcon.Loaded -> {
                            loaded[key] = icon.image
                        }

                        AppIcon.Missing -> {}
                    }
                }
            }
        }

    private sealed interface Pending {
        data class Cached(
            val icon: AppIcon.Loaded,
        ) : Pending

        data class Loading(
            val load: Deferred<AppIcon.Settled>,
        ) : Pending
    }

    private companion object {
        const val LOAD_FACTOR = 0.75f
        const val CAPACITY = 192
    }
}
