package dev.danielgilchrist.knockoff

import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import dev.danielgilchrist.knockoff.listener.KnockoffListener
import dev.danielgilchrist.knockoff.ui.KnockoffScreen
import dev.danielgilchrist.knockoff.ui.MainViewModel
import dev.danielgilchrist.knockoff.ui.theme.KnockoffTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val linking =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KnockoffTheme {
                KnockoffScreen(
                    viewModel = viewModel,
                    onOpenAccessSettings = ::openAccessSettings,
                    onLinkDevice = ::linkDevice,
                    onAllowExactAlarms = ::openExactAlarmSettings,
                )
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

    private fun linkDevice() {
        getSystemService(CompanionDeviceManager::class.java).associate(
            AssociationRequest.Builder().build(),
            mainExecutor,
            object : CompanionDeviceManager.Callback() {
                override fun onAssociationPending(intentSender: IntentSender) {
                    when {
                        lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED) -> {
                            linking.launch(IntentSenderRequest.Builder(intentSender).build())
                        }

                        else -> {
                            Log.w(LOG_TAG, "device link chooser dropped, the screen closed first")
                        }
                    }
                }

                override fun onAssociationCreated(associationInfo: AssociationInfo) {
                    viewModel.refreshSetup()
                }

                override fun onFailure(error: CharSequence?) {
                    Log.w(LOG_TAG, "device link failed: $error")
                }
            },
        )
    }

    private fun openExactAlarmSettings() {
        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", packageName, null)))
    }
}
