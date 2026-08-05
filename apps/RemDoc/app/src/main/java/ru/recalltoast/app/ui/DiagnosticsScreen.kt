package ru.recalltoast.app.ui

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
import androidx.compose.material3.MaterialTheme
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
import ru.recalltoast.app.util.OemHelpProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val help = OemHelpProvider.current(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostics_title)) },
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
            Text(help.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Some firmwares restrict background work. RemDoc cannot bypass OEM limits; use the steps below or reliable mode.",
                modifier = Modifier.padding(vertical = 8.dp)
            )
            help.steps.forEachIndexed { index, step ->
                Text("${index + 1}. $step", modifier = Modifier.padding(vertical = 4.dp))
            }
            TextButton(onClick = {
                runCatching { context.startActivity(OemHelpProvider.openBestIntent(context).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }) { Text("Open suggested settings") }
            TextButton(onClick = {
                context.startActivity(BatteryOptimizationUtils.appDetailsIntent(context))
            }) { Text("Open app details (fallback)") }
            TextButton(onClick = {
                context.startActivity(BatteryOptimizationUtils.batteryOptimizationSettingsIntent())
            }) { Text("Open battery optimization list") }
        }
    }
}
