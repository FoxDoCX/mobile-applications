package ru.recalltoast.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.recalltoast.app.BuildConfig
import ru.recalltoast.app.R
import ru.recalltoast.app.domain.model.DisplayMode
import ru.recalltoast.app.domain.model.ReliabilityMode
import ru.recalltoast.app.domain.model.ThemeMode
import ru.recalltoast.app.service.ReminderForegroundService
import ru.recalltoast.app.ui.components.OverlayPreview
import ru.recalltoast.app.ui.components.SectionTitle
import ru.recalltoast.app.ui.components.SettingsSwitchRow
import ru.recalltoast.app.ui.components.StatusLine
import ru.recalltoast.app.ui.theme.RemDocCyan
import ru.recalltoast.app.ui.theme.RemDocMagenta
import ru.recalltoast.app.util.BatteryOptimizationUtils
import ru.recalltoast.app.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenDebug: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val draft by viewModel.draftText.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val active = settings.enabled && !settings.paused
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.start()
    }
    val phonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setAfterCallEnabled(granted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        Text(
                            text = stringResource(
                                when {
                                    !settings.enabled -> R.string.status_stopped
                                    settings.paused -> R.string.status_paused
                                    else -> R.string.status_active
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleTheme) {
                        Icon(
                            imageVector = if (settings.themeMode == ThemeMode.LIGHT) {
                                Icons.Outlined.DarkMode
                            } else {
                                Icons.Outlined.LightMode
                            },
                            contentDescription = stringResource(R.string.theme_toggle)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.action_settings)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .widthIn(max = 840.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Brush.horizontalGradient(listOf(RemDocCyan, RemDocMagenta)))
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = viewModel::onDraftTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.main_hint)) },
                        minLines = 2,
                        maxLines = 6,
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.flushDraftNow()
                                focusManager.clearFocus()
                            }
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            pluralStringResource(R.plurals.char_count_plural, draft.length, draft.length),
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = viewModel::clearDraftText) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                    if (settings.displayMode == DisplayMode.OVERLAY_CARD) {
                        OverlayPreview(text = draft, style = settings.overlayStyle)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        viewModel.flushDraftNow()
                        viewModel.showNow()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.action_show_now)) }
                Button(
                    onClick = {
                        viewModel.flushDraftNow()
                        if (active) {
                            viewModel.stop()
                        } else {
                            if (settings.reliabilityMode == ReliabilityMode.RELIABLE &&
                                PermissionUtils.needsPostNotificationsPermission() &&
                                !PermissionUtils.hasPostNotificationsPermission(context)
                            ) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.start()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(if (active) R.string.action_stop else R.string.action_start))
                }
            }

            if (settings.reliabilityMode == ReliabilityMode.ECONOMY) {
                Text(
                    stringResource(R.string.economy_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionTitle(stringResource(R.string.triggers_title))
            SettingsSwitchRow(
                title = stringResource(R.string.trigger_unlock),
                checked = settings.unlockEnabled,
                onCheckedChange = viewModel::setUnlockEnabled
            )
            SettingsSwitchRow(
                title = stringResource(R.string.trigger_call),
                checked = settings.afterCallEnabled,
                subtitle = stringResource(R.string.call_permission_rationale),
                onCheckedChange = { enabled ->
                    if (enabled && !PermissionUtils.hasPhoneStatePermission(context)) {
                        phonePermission.launch(Manifest.permission.READ_PHONE_STATE)
                    } else {
                        viewModel.setAfterCallEnabled(enabled)
                    }
                }
            )
            SettingsSwitchRow(
                title = stringResource(R.string.trigger_power),
                checked = settings.powerConnectedEnabled || settings.powerDisconnectedEnabled,
                onCheckedChange = viewModel::setPowerEnabled
            )
            SettingsSwitchRow(
                title = stringResource(R.string.trigger_schedule),
                checked = settings.scheduleEnabled,
                onCheckedChange = viewModel::setScheduleEnabled
            )

            SectionTitle(stringResource(R.string.reminders_title))
            settings.reminders.forEach { reminder ->
                TextButton(onClick = { onOpenEditor(reminder.id) }) {
                    Text(reminder.text.ifBlank { "…" })
                }
            }
            TextButton(onClick = viewModel::addReminder) {
                Text(stringResource(R.string.action_add))
            }

            SectionTitle(stringResource(R.string.status_block_title))
            StatusLine(
                "Mode",
                if (settings.reliabilityMode == ReliabilityMode.RELIABLE) {
                    stringResource(R.string.mode_reliable)
                } else {
                    stringResource(R.string.mode_economy)
                }
            )
            StatusLine(
                stringResource(R.string.overlay_permission),
                stringResource(if (PermissionUtils.canDrawOverlays(context)) R.string.granted else R.string.denied)
            )
            StatusLine(
                stringResource(R.string.phone_permission),
                stringResource(if (PermissionUtils.hasPhoneStatePermission(context)) R.string.granted else R.string.denied)
            )
            StatusLine(
                stringResource(R.string.battery_optimization),
                stringResource(
                    if (BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context)) R.string.ignored
                    else R.string.optimized
                )
            )
            StatusLine(
                stringResource(R.string.service_state),
                stringResource(if (ReminderForegroundService.isRunning) R.string.running else R.string.not_running)
            )
            TextButton(onClick = onOpenPermissions) {
                Text(stringResource(R.string.permissions_title))
            }
            if (BuildConfig.DEBUG) {
                TextButton(onClick = onOpenDebug) {
                    Text(stringResource(R.string.debug_menu))
                }
            }
            Text(
                text = stringResource(R.string.publisher_line),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
