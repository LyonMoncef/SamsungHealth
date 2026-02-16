package com.samsunghealth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsunghealth.ui.SettingsScreen
import com.samsunghealth.ui.SyncScreen
import com.samsunghealth.viewmodel.SyncViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        // ViewModel will re-check permissions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: SyncViewModel = viewModel()
                var showSettings by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    vm.checkSdkAndPermissions()
                }

                // Re-check when returning from permission screen
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            vm.onPermissionsResult()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                if (showSettings) {
                    SettingsScreen(
                        viewModel = vm,
                        onBack = { showSettings = false },
                    )
                } else {
                    SyncScreen(
                        viewModel = vm,
                        onRequestPermissions = {
                            permissionLauncher.launch(vm.healthConnect.permissions)
                        },
                        onNavigateToSettings = { showSettings = true },
                    )
                }
            }
        }
    }
}
