package ru.recalltoast.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.recalltoast.app.R
import ru.recalltoast.app.util.BatteryOptimizationUtils
import ru.recalltoast.app.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val phone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permissions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.overlay_permission_rationale))
            TextButton(onClick = {
                context.startActivity(PermissionUtils.overlaySettingsIntent(context))
            }) { Text(stringResource(R.string.overlay_permission)) }

            Text(stringResource(R.string.notification_permission_rationale))
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notif.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startActivity(PermissionUtils.notificationSettingsIntent(context))
                }
            }) { Text("Notifications") }

            Text(stringResource(R.string.call_permission_rationale))
            TextButton(onClick = { phone.launch(Manifest.permission.READ_PHONE_STATE) }) {
                Text(stringResource(R.string.phone_permission))
            }

            TextButton(onClick = {
                context.startActivity(BatteryOptimizationUtils.batteryOptimizationSettingsIntent())
            }) { Text(stringResource(R.string.battery_optimization)) }

            TextButton(onClick = {
                context.startActivity(PermissionUtils.appDetailsIntent(context))
            }) { Text("App details") }
        }
    }
}
