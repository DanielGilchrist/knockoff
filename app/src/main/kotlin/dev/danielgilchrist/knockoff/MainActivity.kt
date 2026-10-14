package dev.danielgilchrist.knockoff

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.danielgilchrist.knockoff.listener.KnockoffListener
import dev.danielgilchrist.knockoff.ui.KnockoffScreen
import dev.danielgilchrist.knockoff.ui.MainViewModel
import dev.danielgilchrist.knockoff.ui.theme.KnockoffTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KnockoffTheme {
                KnockoffScreen(viewModel = viewModel, onOpenAccessSettings = ::openAccessSettings)
            }
        }
    }

    private fun openAccessSettings() {
        val listener = ComponentName(this, KnockoffListener::class.java)
        val detail =
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener.flattenToString())

        try {
            startActivity(detail)
        } catch (exception: ActivityNotFoundException) {
            Log.w(LOG_TAG, "listener detail settings unavailable, opening the list: $exception")
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
