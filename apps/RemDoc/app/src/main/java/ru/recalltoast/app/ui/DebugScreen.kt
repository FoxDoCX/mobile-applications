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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.recalltoast.app.BuildConfig
import ru.recalltoast.app.R
import ru.recalltoast.app.domain.model.ReminderPosition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    if (!BuildConfig.DEBUG) {
        onBack()
        return
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_menu)) },
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
            TextButton(onClick = viewModel::debugSimulateUnlock) { Text("Simulate unlock") }
            TextButton(onClick = viewModel::debugSimulateCallEnd) { Text("Simulate call end") }
            TextButton(onClick = viewModel::showNow) { Text("Show Toast/Card now") }
            TextButton(onClick = {
                viewModel.setOverlayStyle(settings.overlayStyle.copy(position = ReminderPosition.TOP))
                viewModel.showNow()
            }) { Text("Preview top") }
            TextButton(onClick = {
                viewModel.setOverlayStyle(settings.overlayStyle.copy(position = ReminderPosition.CENTER))
                viewModel.showNow()
            }) { Text("Preview center") }
            TextButton(onClick = {
                viewModel.setOverlayStyle(settings.overlayStyle.copy(position = ReminderPosition.BOTTOM))
                viewModel.showNow()
            }) { Text("Preview bottom") }
            TextButton(onClick = viewModel::debugResetOnboarding) { Text("Reset onboarding") }
            Text("DataStore snapshot:")
            Text(settings.toString())
        }
    }
}
