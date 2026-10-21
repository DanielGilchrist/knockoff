package dev.danielgilchrist.knockoff.ui.appicon

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppIconsTest {
    private val slack = IconKey(PackageName("com.slack"), 40)
    private val gmail = IconKey(PackageName("com.google.android.gm"), 40)
    private val whatsapp = IconKey(PackageName("com.whatsapp"), 40)
    private val image = FakeImage()

    private class CountingSource(
        private val answer: suspend (IconKey) -> AppIcon.Settled,
    ) : IconSource {
        val loads = mutableListOf<IconKey>()

        override suspend fun load(key: IconKey): AppIcon.Settled {
            loads += key
            return answer(key)
        }
    }

    @Test
    fun `a loaded icon is cached and a missing one is tried again`() =
        runTest {
            val source = CountingSource { if (it == slack) AppIcon.Loaded(image) else AppIcon.Missing }
            val icons = AppIcons(backgroundScope, source)

            assertEquals(AppIcon.Loading, icons.current(slack))
            assertEquals(AppIcon.Loaded(image), icons.load(slack))
            assertEquals(AppIcon.Loaded(image), icons.load(slack))
            assertEquals(AppIcon.Loaded(image), icons.current(slack))
            assertEquals(AppIcon.Missing, icons.load(gmail))
            assertEquals(AppIcon.Missing, icons.load(gmail))
            assertEquals(AppIcon.Loading, icons.current(gmail))
            assertEquals(listOf(slack, gmail, gmail), source.loads)
        }

    @Test
    fun `callers asking at once share one load`() =
        runTest {
            val release = CompletableDeferred<Unit>()
            val source =
                CountingSource {
                    release.await()
                    AppIcon.Loaded(image)
                }
            val icons = AppIcons(backgroundScope, source)

            val first = async { icons.load(slack) }
            val second = async { icons.load(slack) }
            testScheduler.runCurrent()
            release.complete(Unit)

            assertEquals(listOf(AppIcon.Loaded(image), AppIcon.Loaded(image)), listOf(first.await(), second.await()))
            assertEquals(listOf(slack), source.loads)
        }

    @Test
    fun `a cancelled caller still leaves the icon cached`() =
        runTest {
            val release = CompletableDeferred<Unit>()
            val icons =
                AppIcons(
                    backgroundScope,
                    CountingSource {
                        release.await()
                        AppIcon.Loaded(image)
                    },
                )

            val waiting = async { icons.load(slack) }
            testScheduler.runCurrent()
            waiting.cancel()
            release.complete(Unit)
            testScheduler.runCurrent()

            assertEquals(AppIcon.Loaded(image), icons.current(slack))
        }

    @Test
    fun `a changed package loads again and other sizes of it go too`() =
        runTest {
            val source = CountingSource { AppIcon.Loaded(image) }
            val icons = AppIcons(backgroundScope, source)
            val small = slack.copy(px = 20)

            icons.load(slack)
            icons.load(small)
            icons.load(gmail)
            icons.forget(slack.app)

            assertEquals(
                listOf(AppIcon.Loading, AppIcon.Loading, AppIcon.Loaded(image)),
                listOf(slack, small, gmail).map(icons::current),
            )
            icons.load(slack)
            assertEquals(listOf(slack, small, gmail, slack), source.loads)
        }

    @Test
    fun `a load started before its package changed doesn't overwrite a newer one`() =
        runTest {
            val stale = CompletableDeferred<Unit>()
            val fresh = FakeImage()
            var calls = 0
            val icons =
                AppIcons(
                    backgroundScope,
                    CountingSource {
                        calls += 1
                        when (calls) {
                            1 -> {
                                stale.await()
                                AppIcon.Loaded(image)
                            }

                            else -> {
                                AppIcon.Loaded(fresh)
                            }
                        }
                    },
                )

            val first = async { icons.load(slack) }
            testScheduler.runCurrent()
            icons.forget(slack.app)
            assertEquals(AppIcon.Loaded(fresh), icons.load(slack))
            stale.complete(Unit)
            first.await()

            assertEquals(AppIcon.Loaded(fresh), icons.current(slack))
        }

    @Test
    fun `forgetting an app moves only its generation on`() =
        runTest {
            val icons = AppIcons(backgroundScope, CountingSource { AppIcon.Missing })

            icons.forget(slack.app)
            icons.forget(slack.app)

            assertEquals(listOf(2, 0), listOf(slack.app, gmail.app).map(icons.generations.value::of))
        }

    @Test
    fun `the least recently used icon is dropped past capacity`() =
        runTest {
            val icons = AppIcons(backgroundScope, { _: IconKey -> AppIcon.Loaded(image) }, capacity = 2)

            icons.load(slack)
            icons.load(gmail)
            icons.current(slack)
            icons.load(whatsapp)

            assertEquals(
                listOf(AppIcon.Loaded(image), AppIcon.Loading, AppIcon.Loaded(image)),
                listOf(slack, gmail, whatsapp).map(icons::current),
            )
        }

    private class FakeImage : ImageBitmap {
        override val width: Int = 1
        override val height: Int = 1
        override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888
        override val hasAlpha: Boolean = true
        override val colorSpace: ColorSpace = ColorSpaces.Srgb

        override fun prepareToDraw() = Unit

        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int,
        ) = Unit
    }
}
