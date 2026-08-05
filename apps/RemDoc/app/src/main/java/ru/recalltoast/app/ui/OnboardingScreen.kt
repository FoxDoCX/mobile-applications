package ru.recalltoast.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.recalltoast.app.R
import ru.recalltoast.app.domain.model.DisplayMode
import ru.recalltoast.app.domain.model.ReliabilityMode

@Composable
fun OnboardingScreen(onFinished: (ReliabilityMode, DisplayMode) -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(ReliabilityMode.ECONOMY) }
    var display by remember { mutableStateOf(DisplayMode.SYSTEM_TOAST) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (page) {
            0 -> {
                Text(stringResource(R.string.onboarding_title_1), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.onboarding_body_1), style = MaterialTheme.typography.bodyLarge)
            }
            1 -> {
                Text(stringResource(R.string.onboarding_title_2), style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == ReliabilityMode.RELIABLE,
                        onClick = { mode = ReliabilityMode.RELIABLE },
                        label = { Text(stringResource(R.string.mode_reliable)) }
                    )
                    FilterChip(
                        selected = mode == ReliabilityMode.ECONOMY,
                        onClick = { mode = ReliabilityMode.ECONOMY },
                        label = { Text(stringResource(R.string.mode_economy)) }
                    )
                }
                Text(
                    stringResource(
                        if (mode == ReliabilityMode.RELIABLE) R.string.onboarding_body_2_reliable
                        else R.string.onboarding_body_2_economy
                    )
                )
            }
            else -> {
                Text(stringResource(R.string.onboarding_title_3), style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = display == DisplayMode.SYSTEM_TOAST,
                        onClick = { display = DisplayMode.SYSTEM_TOAST },
                        label = { Text("Toast") }
                    )
                    FilterChip(
                        selected = display == DisplayMode.OVERLAY_CARD,
                        onClick = { display = DisplayMode.OVERLAY_CARD },
                        label = { Text("Card") }
                    )
                }
                Text(
                    stringResource(
                        if (display == DisplayMode.SYSTEM_TOAST) R.string.onboarding_body_3_toast
                        else R.string.onboarding_body_3_card
                    )
                )
            }
        }
        Button(
            onClick = {
                if (page < 2) page++ else onFinished(mode, display)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(if (page < 2) R.string.continue_label else R.string.done_label))
        }
    }
}
