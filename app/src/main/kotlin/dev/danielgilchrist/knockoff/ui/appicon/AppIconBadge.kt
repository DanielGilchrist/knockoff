package dev.danielgilchrist.knockoff.ui.appicon

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.ui.components.BADGE_SIZE
import dev.danielgilchrist.knockoff.ui.components.IconBadge

private const val FADE_MS = 150

@Composable
internal fun AppIconBadge(
    app: PackageName,
    modifier: Modifier = Modifier,
    size: Dp = BADGE_SIZE,
) {
    val icons = LocalAppIcons.current
    val iconKey = IconKey(app, with(LocalDensity.current) { size.roundToPx() })
    val generations by icons.generations.collectAsState()

    key(iconKey, generations.of(app)) {
        val icon by produceState(initialValue = icons.current(iconKey)) { value = icons.load(iconKey) }

        Crossfade(
            targetState = icon,
            animationSpec = tween(FADE_MS),
            modifier =
                modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            label = "app icon",
        ) {
            when (it) {
                is AppIcon.Loaded -> {
                    Image(bitmap = it.image, contentDescription = null, modifier = Modifier.fillMaxSize())
                }

                AppIcon.Loading, AppIcon.Missing -> {
                    IconBadge(icon = R.drawable.ic_apps, size = size)
                }
            }
        }
    }
}
