package ru.recalltoast.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.recalltoast.app.R
import ru.recalltoast.app.domain.model.DisplayMode
import ru.recalltoast.app.domain.model.OverlayAnimation
import ru.recalltoast.app.domain.model.OverlayStyle
import ru.recalltoast.app.domain.model.ReliabilityMode
import ru.recalltoast.app.domain.model.ReminderPosition
import ru.recalltoast.app.domain.model.SelectionMode
import ru.recalltoast.app.domain.model.ThemeMode
import ru.recalltoast.app.ui.components.OverlayPreview
import ru.recalltoast.app.ui.components.SectionTitle
import ru.recalltoast.app.ui.components.SettingsSwitchRow
import ru.recalltoast.app.util.BatteryOptimizationUtils
import ru.recalltoast.app.util.OemHelpProvider
import ru.recalltoast.app.util.PermissionUtils
import java.nio.charset.Charset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmImport by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = viewModel.exportSettingsJson()
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(json.toByteArray(Charset.forName("UTF-8")))
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val raw = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charset.forName("UTF-8"))
            } ?: return@launch
            confirmImport = raw
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                .widthIn(max = 840.dp)
        ) {
            SectionTitle(stringResource(R.string.section_basic))
            SettingsSwitchRow("Enabled", settings.enabled) { viewModel.updateSettings { s -> s.copy(enabled = it) } }
            SettingsSwitchRow(
                stringResource(R.string.mode_reliable),
                settings.reliabilityMode == ReliabilityMode.RELIABLE,
                stringResource(R.string.economy_warning)
            ) { checked ->
                viewModel.setReliabilityMode(if (checked) ReliabilityMode.RELIABLE else ReliabilityMode.ECONOMY)
            }
            SettingsSwitchRow(stringResource(R.string.trigger_unlock), settings.unlockEnabled, onCheckedChange = viewModel::setUnlockEnabled)
            Text("Unlock delay: ${settings.unlockDelaySeconds}s")
            Slider(
                value = settings.unlockDelaySeconds.toFloat(),
                onValueChange = {
                    viewModel.updateSettings { s -> s.copy(unlockDelaySeconds = it.toInt()) }
                },
                valueRange = 0f..10f,
                steps = 9
            )
            Text("Cooldown: ${settings.globalCooldownSeconds}s")
            Slider(
                value = settings.globalCooldownSeconds.toFloat(),
                onValueChange = {
                    viewModel.updateSettings { s -> s.copy(globalCooldownSeconds = it.toInt()) }
                },
                valueRange = 0f..60f
            )
            TextButton(onClick = { viewModel.pause(15) }) { Text(stringResource(R.string.pause_15m)) }
            TextButton(onClick = { viewModel.pause(60) }) { Text(stringResource(R.string.pause_1h)) }
            TextButton(onClick = { viewModel.pause(untilTomorrow = true) }) { Text(stringResource(R.string.pause_tomorrow)) }
            TextButton(onClick = viewModel::resume) { Text(stringResource(R.string.resume)) }
            SettingsSwitchRow("Respect DND", settings.respectDnd) {
                viewModel.updateSettings { s -> s.copy(respectDnd = it) }
            }
            SettingsSwitchRow("Skip during call", settings.skipDuringCall) {
                viewModel.updateSettings { s -> s.copy(skipDuringCall = it) }
            }
            SettingsSwitchRow("Skip when screen off", settings.skipWhenScreenOff) {
                viewModel.updateSettings { s -> s.copy(skipWhenScreenOff = it) }
            }

            SectionTitle(stringResource(R.string.section_appearance))
            SettingsSwitchRow(
                "Overlay card",
                settings.displayMode == DisplayMode.OVERLAY_CARD,
                stringResource(R.string.overlay_permission_rationale)
            ) { checked ->
                viewModel.setDisplayMode(if (checked) DisplayMode.OVERLAY_CARD else DisplayMode.SYSTEM_TOAST)
                if (checked && !PermissionUtils.canDrawOverlays(context)) {
                    context.startActivity(PermissionUtils.overlaySettingsIntent(context))
                }
            }
            OverlayPreview(settings.reminders.firstOrNull()?.text.orEmpty(), settings.overlayStyle)
            AppearanceControls(settings.overlayStyle, viewModel::setOverlayStyle)
            TextButton(onClick = viewModel::restoreOverlayDefaults) {
                Text(stringResource(R.string.restore_defaults))
            }
            TextButton(onClick = { viewModel.setTheme(ThemeMode.SYSTEM) }) { Text(stringResource(R.string.theme_system)) }
            TextButton(onClick = { viewModel.setTheme(ThemeMode.LIGHT) }) { Text(stringResource(R.string.theme_light)) }
            TextButton(onClick = { viewModel.setTheme(ThemeMode.DARK) }) { Text(stringResource(R.string.theme_dark)) }

            TextButton(onClick = { advanced = !advanced }) {
                Text(stringResource(R.string.advanced))
            }
            if (advanced) {
                SectionTitle(stringResource(R.string.section_logic))
                TextButton(onClick = { viewModel.setSelectionMode(SelectionMode.SINGLE) }) { Text("Single") }
                TextButton(onClick = { viewModel.setSelectionMode(SelectionMode.ROUND_ROBIN) }) { Text("Round-robin") }
                TextButton(onClick = { viewModel.setSelectionMode(SelectionMode.RANDOM) }) { Text("Random") }
                TextButton(onClick = { viewModel.setSelectionMode(SelectionMode.RANDOM_NO_IMMEDIATE_REPEAT) }) { Text("Random no repeat") }
                TextButton(onClick = { viewModel.setSelectionMode(SelectionMode.PRIORITY) }) { Text("Priority") }

                SectionTitle(stringResource(R.string.section_system))
                SettingsSwitchRow("Autostart after reboot", settings.autostartAfterBoot) {
                    viewModel.updateSettings { s -> s.copy(autostartAfterBoot = it) }
                }
                TextButton(onClick = {
                    context.startActivity(PermissionUtils.overlaySettingsIntent(context))
                }) { Text("Open overlay permission") }
                TextButton(onClick = {
                    context.startActivity(PermissionUtils.notificationSettingsIntent(context))
                }) { Text("Open notification settings") }
                TextButton(onClick = {
                    context.startActivity(BatteryOptimizationUtils.batteryOptimizationSettingsIntent())
                }) { Text("Open battery settings") }
                TextButton(onClick = onOpenDiagnostics) { Text(stringResource(R.string.diagnostics_title)) }
                TextButton(onClick = { exportLauncher.launch("recalltoast-settings.json") }) {
                    Text(stringResource(R.string.export_settings))
                }
                TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }) {
                    Text(stringResource(R.string.import_settings))
                }
                TextButton(onClick = { confirmClear = true }) {
                    Text(stringResource(R.string.clear_data))
                }
            }

            TextButton(onClick = onOpenPrivacy) { Text(stringResource(R.string.privacy_title)) }
            TextButton(onClick = onOpenAbout) { Text(stringResource(R.string.about_title)) }
        }
    }

    confirmImport?.let { raw ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text(stringResource(R.string.import_settings)) },
            text = { Text("Replace current settings with imported file?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.importSettingsJson(raw)
                        confirmImport = null
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_data)) },
            text = { Text("Delete all local reminders and settings?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    confirmClear = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun AppearanceControls(style: OverlayStyle, onChange: (OverlayStyle) -> Unit) {
    Text("Position")
    RowButtons(
        listOf(
            "Top" to ReminderPosition.TOP,
            "Center" to ReminderPosition.CENTER,
            "Bottom" to ReminderPosition.BOTTOM
        )
    ) { onChange(style.copy(position = it)) }
    Text("Duration: ${style.durationMillis} ms")
    Slider(
        value = style.durationMillis.toFloat(),
        onValueChange = { onChange(style.copy(durationMillis = it.toLong())) },
        valueRange = 1500f..8000f
    )
    Text("Text size: ${style.textSizeSp.toInt()} sp")
    Slider(
        value = style.textSizeSp,
        onValueChange = { onChange(style.copy(textSizeSp = it)) },
        valueRange = 12f..28f
    )
    Text("Corner: ${style.cornerRadiusDp.toInt()} dp")
    Slider(
        value = style.cornerRadiusDp,
        onValueChange = { onChange(style.copy(cornerRadiusDp = it)) },
        valueRange = 0f..32f
    )
    Text("Opacity: ${(style.alpha * 100).toInt()}%")
    Slider(
        value = style.alpha,
        onValueChange = { onChange(style.copy(alpha = it)) },
        valueRange = 0.4f..1f
    )
    SettingsSwitchRow("Bold", style.bold) { onChange(style.copy(bold = it)) }
    SettingsSwitchRow("Close button", style.showCloseButton) { onChange(style.copy(showCloseButton = it)) }
    SettingsSwitchRow("Progress", style.showProgress) { onChange(style.copy(showProgress = it)) }
    SettingsSwitchRow("Icon", style.showIcon) { onChange(style.copy(showIcon = it)) }
    TextButton(onClick = { onChange(style.copy(animation = OverlayAnimation.FADE)) }) { Text("Fade") }
    TextButton(onClick = { onChange(style.copy(animation = OverlayAnimation.SLIDE)) }) { Text("Slide") }
    TextButton(onClick = { onChange(style.copy(animation = OverlayAnimation.NONE)) }) { Text("No animation") }
    // Simple color presets without third-party picker
    TextButton(onClick = {
        onChange(style.copy(backgroundColor = 0xFF141B33, textColor = 0xFFFFFFFF))
    }) { Text("Dark navy") }
    TextButton(onClick = {
        onChange(style.copy(backgroundColor = 0xFF0A0E21, textColor = 0xFF00E5FF))
    }) { Text("Cyan on navy") }
    TextButton(onClick = {
        onChange(style.copy(backgroundColor = 0xFFF4F6FB, textColor = 0xFF12162A))
    }) { Text("Light preset") }
}

@Composable
private fun <T> RowButtons(items: List<Pair<String, T>>, onPick: (T) -> Unit) {
    items.forEach { (label, value) ->
        TextButton(onClick = { onPick(value) }, modifier = Modifier.fillMaxWidth()) {
            Text(label)
        }
    }
}
