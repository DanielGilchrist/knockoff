package dev.danielgilchrist.knockoff.ui.appicon

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackageIconSource(
    private val packageManager: PackageManager,
) : IconSource {
    override suspend fun load(key: IconKey): AppIcon.Settled =
        withContext(Dispatchers.IO) {
            try {
                AppIcon.Loaded(packageManager.getApplicationIcon(key.app.value).rendered(key.px).asImageBitmap())
            } catch (_: PackageManager.NameNotFoundException) {
                AppIcon.Missing
            }
        }

    private fun Drawable.rendered(px: Int): Bitmap =
        Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).also { bitmap ->
            setBounds(0, 0, px, px)
            draw(Canvas(bitmap))
        }
}
