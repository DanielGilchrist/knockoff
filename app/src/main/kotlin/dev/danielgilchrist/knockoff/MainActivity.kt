package dev.danielgilchrist.knockoff

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielgilchrist.knockoff.listener.KnockoffListener
import dev.danielgilchrist.knockoff.ui.Appearance
import dev.danielgilchrist.knockoff.ui.KnockoffScreen
import dev.danielgilchrist.knockoff.ui.MainViewModel
import dev.danielgilchrist.knockoff.ui.NearbyAsk
import dev.danielgilchrist.knockoff.ui.PairedDevice
import dev.danielgilchrist.knockoff.ui.SystemActions
import dev.danielgilchrist.knockoff.ui.theme.KnockoffTheme
import dev.danielgilchrist.knockoff.ui.theme.isDark

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val linking =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
    private val nearbyDevices =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.nearbyDevicesAnswered(
                NearbyAsk.after(granted, shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)),
            )
        }
    private val bluetoothChanges =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                viewModel.refreshPaired()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.editor.settings.collectAsStateWithLifecycle()

            when (val appearance = Appearance.of(settings)) {
                Appearance.Pending -> {}

                is Appearance.Themed -> {
                    val dark = appearance.theme.isDark()

                    SideEffect {
                        val bars = barStyle(dark)
                        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                    }
                    KnockoffTheme(dark = dark) {
                        KnockoffScreen(
                            viewModel = viewModel,
                            system =
                                SystemActions(
                                    onOpenAccessSettings = ::openAccessSettings,
                                    onAllowNotifications = ::openNotificationSettings,
                                    onAllowNearbyDevices = {
                                        nearbyDevices.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                    },
                                    onOpenBluetoothSettings = {
                                        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                    },
                                    onOpenAppSettings = {
                                        startActivity(
                                            Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", packageName, null),
                                            ),
                                        )
                                    },
                                    onLinkPaired = { device -> link(pairedRequest(device)) },
                                    onLinkNearby = { link(AssociationRequest.Builder().build()) },
                                ),
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(
            bluetoothChanges,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            },
            RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStop() {
        unregisterReceiver(bluetoothChanges)
        super.onStop()
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

    private fun pairedRequest(device: PairedDevice): AssociationRequest =
        AssociationRequest
            .Builder()
            .setSingleDevice(true)
            .addDeviceFilter(BluetoothDeviceFilter.Builder().setAddress(device.address.value).build())
            .build()

    private fun link(request: AssociationRequest) {
        getSystemService(CompanionDeviceManager::class.java).associate(
            request,
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

    private fun barStyle(dark: Boolean): SystemBarStyle =
        when {
            dark -> SystemBarStyle.dark(Color.TRANSPARENT)
            else -> SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    }
}
