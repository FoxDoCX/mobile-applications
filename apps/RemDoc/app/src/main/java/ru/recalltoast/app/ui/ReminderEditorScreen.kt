package ru.recalltoast.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.recalltoast.app.R
import ru.recalltoast.app.domain.model.DisplayMode
import ru.recalltoast.app.domain.model.ReminderPosition
import ru.recalltoast.app.domain.model.ReminderTrigger
import ru.recalltoast.app.ui.components.OverlayPreview
import ru.recalltoast.app.ui.components.SettingsSwitchRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorScreen(
    reminderId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val reminder = settings.reminders.firstOrNull { it.id == reminderId }
    if (reminder == null) {
        onBack()
        return
    }
    var text by remember(reminder.id) { mutableStateOf(reminder.text) }
    var enabled by remember(reminder.id) { mutableStateOf(reminder.enabled) }

    LaunchedEffect(reminder.id) {
        text = reminder.text
        enabled = reminder.enabled
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.upsertReminder(
                            reminder.copy(
                                text = text,
                                enabled = enabled,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        onBack()
                    }) { Text(stringResource(R.string.action_save)) }
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
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.main_hint)) },
                minLines = 3,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
            SettingsSwitchRow("Enabled", enabled) { enabled = it }
            SettingsSwitchRow(
                "Unlock trigger",
                ReminderTrigger.UNLOCK in reminder.triggers
            ) { checked ->
                val triggers = reminder.triggers.toMutableSet()
                if (checked) triggers.add(ReminderTrigger.UNLOCK) else triggers.remove(ReminderTrigger.UNLOCK)
                viewModel.upsertReminder(reminder.copy(triggers = triggers, text = text, enabled = enabled))
            }
            TextButton(onClick = {
                viewModel.upsertReminder(reminder.copy(displayMode = DisplayMode.SYSTEM_TOAST, text = text))
            }) { Text("Force Toast") }
            TextButton(onClick = {
                viewModel.upsertReminder(reminder.copy(displayMode = DisplayMode.OVERLAY_CARD, text = text))
            }) { Text("Force Card") }
            TextButton(onClick = {
                viewModel.upsertReminder(reminder.copy(position = ReminderPosition.TOP, text = text))
            }) { Text("Top") }
            TextButton(onClick = {
                viewModel.upsertReminder(reminder.copy(position = ReminderPosition.CENTER, text = text))
            }) { Text("Center") }
            TextButton(onClick = {
                viewModel.upsertReminder(reminder.copy(position = ReminderPosition.BOTTOM, text = text))
            }) { Text("Bottom") }
            OverlayPreview(text, settings.overlayStyle)
            TextButton(onClick = {
                viewModel.deleteReminder(reminder.id)
                onBack()
            }) { Text("Delete") }
            TextButton(onClick = {
                viewModel.upsertReminder(reminder.copy(text = text, enabled = enabled))
                viewModel.showNow()
            }) { Text(stringResource(R.string.preview)) }
        }
    }
}
