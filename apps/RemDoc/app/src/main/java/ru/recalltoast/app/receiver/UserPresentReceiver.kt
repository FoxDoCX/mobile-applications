package ru.recalltoast.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.recalltoast.app.RecallApplication
import ru.recalltoast.app.domain.model.ReliabilityMode
import ru.recalltoast.app.domain.model.ReminderTrigger
import ru.recalltoast.app.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Economy-mode unlock listener. Manifest-registered because ACTION_USER_PRESENT
 * is exempt from Android 8+ implicit broadcast limits.
 * Reliable mode uses a dynamic receiver inside the foreground service instead.
 */
class UserPresentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_USER_PRESENT) return
        val app = context.applicationContext as? RecallApplication ?: return
        // Keep receiver work short: schedule presentation and return.
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = app.settingsRepository.getSettings()
                if (!settings.enabled || settings.paused) return@launch
                if (settings.reliabilityMode == ReliabilityMode.RELIABLE) {
                    // FGS dynamic receiver handles unlock in reliable mode.
                    return@launch
                }
                app.reminderPresenter.showForTrigger(ReminderTrigger.UNLOCK)
            } catch (t: Throwable) {
                Logger.e("USER_PRESENT handling failed", t)
            }
        }
    }
}
